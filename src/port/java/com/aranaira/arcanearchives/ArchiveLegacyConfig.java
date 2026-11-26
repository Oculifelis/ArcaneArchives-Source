package com.aranaira.arcanearchives;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Legacy restart settings shared by the client and dedicated server. */
public final class ArchiveLegacyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ARSENAL,RESONATOR_COMPLETE,RESONATOR_TICKING,BRAZIER_PICKUP;
    public static final ModConfigSpec.DoubleValue RESONATOR_VOLUME;
    static {
        var b=new ModConfigSpec.Builder();
        ARSENAL=b.comment("Keep gems registered, but hide them from creative/JEI and omit gem/socket recipes when false. Restart required.").worldRestart().define("enableArsenal",true);
        RESONATOR_COMPLETE=b.define("resonatorCompleteSound",true);
        RESONATOR_TICKING=b.define("resonatorTickingSound",true);
        RESONATOR_VOLUME=b.defineInRange("resonatorTickVolume",.15,0,1);
        BRAZIER_PICKUP=b.define("brazierPickupSound",true);
        SPEC=b.build();
    }
    public static boolean arsenal(){return !SPEC.isLoaded()||ARSENAL.get();}
    public static boolean arsenalItem(net.minecraft.world.item.Item item){return item instanceof GemItem||item==AA.item("gemsocket");}
    public static void guideFlag(){
        try {
            var api=Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
            Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI").getMethod("setConfigFlag",String.class,boolean.class).invoke(api,"arcanearchives:arsenal_enabled",arsenal());
        } catch(ClassNotFoundException absent) { /* Patchouli is optional. */ }
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Could not set Patchouli's Arsenal flag",failure);}
    }
}
