package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ImpactReactor extends PowerGenerator{
    public final int timerUse = timers++;

    public int plasmas = 4;
    public float warmupSpeed = 0.001f;
    public float itemDuration = 60f;
    public int explosionRadius = 50;
    public int explosionDamage = 2000;

    public Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
    public int bottomRegion;
    public int[] plasmaRegions;

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;
        bottomRegion = reg("-bottom");
        plasmaRegions = new int[plasmas];
    }

    @Override
    public void load(){
        super.load();

        for(int i = 0; i < plasmas; i++){
            plasmaRegions[i] = reg("-plasma-" + i);
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("poweroutput", entity -> new Bar(() ->
        Core.bundle.format("bar.poweroutput",
        Strings.fixed(Math.max(entity.getPowerProduction() - consumes.getPower().usage, 0) * 60 * entity.timeScale(), 1)),
        () -> Pal.powerBar,
        () -> ((GeneratorEntity)entity).productionEfficiency));
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(BlockStat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name)};
    }

    public class FusionReactorEntity extends GeneratorEntity{
        public float warmup;


        @Override
        public void updateTile(){
            if(consValid() && power.status >= 0.99f){
                boolean prevOut = getPowerProduction() <= consumes.getPower().requestedPower(this);

                warmup = Mathf.lerpDelta(warmup, 1f, warmupSpeed);
                if(Mathf.equal(warmup, 1f, 0.001f)){
                    warmup = 1f;
                }

                if(!prevOut && (getPowerProduction() > consumes.getPower().requestedPower(this))){
                    Events.fire(Trigger.impactPower);
                }

                if(timer(timerUse, itemDuration / timeScale())){
                    consume();
                }
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.01f);
            }

            productionEfficiency = Mathf.pow(warmup, 5f);
        }

        @Override
        public void draw(){
            Draw.rect(reg(bottomRegion), x, y);

            for(int i = 0; i < plasmas; i++){
                float r = size * tilesize - 3f + Mathf.absin(Time.time(), 2f + i * 1f, 5f - i * 0.5f);

                Draw.color(plasma1, plasma2, (float)i / plasmas);
                Draw.alpha((0.3f + Mathf.absin(Time.time(), 2f + i * 2f, 0.3f + i * 0.05f)) * warmup);
                Draw.blend(Blending.additive);
                Draw.rect(reg(plasmaRegions[i]), x, y, r, r, Time.time() * (12 + i * 6f) * warmup);
                Draw.blend();
            }

            Draw.color();

            Draw.rect(region, x, y);

            Draw.color();
        }

        @Override
        public void drawLight(){
            float fract = tile.<FusionReactorEntity>ent().warmup;
            renderer.lights.add(x, y, (110f + Mathf.absin(5, 5f)) * fract, Tmp.c1.set(plasma2).lerp(plasma1, Mathf.absin(7f, 0.2f)), 0.8f * fract);
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();

            if(warmup < 0.4f || !state.rules.reactorExplosions) return;

            Sounds.explosionbig.at(tile);

            Effects.shake(6f, 16f, x, y);
            Fx.impactShockwave.at(x, y);
            for(int i = 0; i < 6; i++){
                Time.run(Mathf.random(80), () -> Fx.impactcloud.at(x, y));
            }

            Damage.damage(x, y, explosionRadius * tilesize, explosionDamage * 4);


            for(int i = 0; i < 20; i++){
                Time.run(Mathf.random(80), () -> {
                    Tmp.v1.rnd(Mathf.random(40f));
                    Fx.explosion.at(Tmp.v1.x + x, Tmp.v1.y + y);
                });
            }

            for(int i = 0; i < 70; i++){
                Time.run(Mathf.random(90), () -> {
                    Tmp.v1.rnd(Mathf.random(120f));
                    Fx.impactsmoke.at(Tmp.v1.x + x, Tmp.v1.y + y);
                });
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
        }
    }
}
