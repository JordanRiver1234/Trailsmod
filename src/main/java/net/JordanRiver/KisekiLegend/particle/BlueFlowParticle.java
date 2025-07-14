package net.JordanRiver.KisekiLegend.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class BlueFlowParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BlueFlowParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;

        // Set particle properties
        this.lifetime = 40 + this.random.nextInt(20); // 2-3 seconds
        this.scale(0.1f + this.random.nextFloat() * 0.1f);
        this.setAlpha(0.8f + this.random.nextFloat() * 0.2f);

        // Blue color with slight variation
        this.rCol = 0.2f + this.random.nextFloat() * 0.2f;
        this.gCol = 0.4f + this.random.nextFloat() * 0.3f;
        this.bCol = 0.8f + this.random.nextFloat() * 0.2f;

        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        // Fade out over time
        this.alpha = Mth.lerp(((float)this.age / (float)this.lifetime), this.alpha, 0.0f);

        // Gentle floating motion
        this.yd += 0.005; // Slow upward drift
        this.xd *= 0.98; // Slow down horizontal movement
        this.zd *= 0.98;

        // Add some swirl motion
        double swirl = Math.sin(this.age * 0.1) * 0.001;
        this.xd += swirl;
        this.zd += Math.cos(this.age * 0.1) * 0.001;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new BlueFlowParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}