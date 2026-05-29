package net.mycar.client.sound;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;

/**
 * Looping siren sound attached to an emergency vehicle.
 *
 * <p>Unlike {@code World.playSound}, which fires once at a fixed world
 * position, this is a {@link MovingSoundInstance}: its {@link #tick()} runs
 * every client tick and pushes the sound's x/y/z forward to wherever the
 * vehicle is right now. That fixes the "siren stays behind when you drive
 * away" problem we had with position-fixed plays.</p>
 *
 * <p>The instance auto-terminates (returns {@code isDone() == true}) when
 * the vehicle is removed from the world or the driver flips the siren off.
 * The sound manager will then stop and discard it. {@link
 * net.mycar.client.MyCarClient} is in charge of constructing one per active
 * emergency vehicle and won't re-spawn while a live instance exists.</p>
 */
public class SirenSoundInstance extends MovingSoundInstance {
    private final AbstractVehicleEntity vehicle;

    public SirenSoundInstance(AbstractVehicleEntity vehicle) {
        super(MyCarMod.SIREN_SOUND, SoundCategory.NEUTRAL);
        this.vehicle = vehicle;
        // Loop the OGG continuously — no gap between repeats. Volume is
        // boosted (1.6) to extend audible range so other players hear it
        // from a reasonable distance.
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 1.6F;
        this.pitch = 1.0F;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    @Override
    public void tick() {
        if (vehicle.removed || !vehicle.isSirenActive()) {
            this.setDone();
            return;
        }
        // Track the vehicle's current position every tick so the sound moves
        // with it. Without this, the siren would stay where it started.
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }
}
