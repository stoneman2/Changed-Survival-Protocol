package net.stonenibbler.changed_survive_protocol.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class CSPConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private CSPConfig() {
    }

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static final class Common {
        public final ForgeConfigSpec.DoubleValue waterCoverageWashPerTick;
        public final ForgeConfigSpec.DoubleValue rainCoverageWashPerTick;
        public final ForgeConfigSpec.IntValue passiveCoverageDecayIntervalTicks;
        public final ForgeConfigSpec.DoubleValue passiveCoverageDecayAmount;
        public final ForgeConfigSpec.DoubleValue coverageInfectionThreshold;
        public final ForgeConfigSpec.DoubleValue infectionStartPercent;
        public final ForgeConfigSpec.IntValue infectionGrowthIntervalTicks;
        public final ForgeConfigSpec.DoubleValue infectionGrowthPerInterval;
        public final ForgeConfigSpec.DoubleValue infectionFromExtraCoverageMultiplier;

        public final ForgeConfigSpec.DoubleValue disinfectantWipeCoverageRemoval;
        public final ForgeConfigSpec.DoubleValue disinfectantSprayCoverageRemoval;
        public final ForgeConfigSpec.IntValue inhibitorTicks;
        public final ForgeConfigSpec.DoubleValue inhibitorInfectionRemoval;
        public final ForgeConfigSpec.DoubleValue sampleExtractionDamage;
        public final ForgeConfigSpec.DoubleValue sampleExtractionMinHealth;
        public final ForgeConfigSpec.DoubleValue sampleExtractionInfectionRemoval;
        public final ForgeConfigSpec.DoubleValue cureDoseInfectionRemoval;
        public final ForgeConfigSpec.IntValue cureDoseSuppressantTicks;

        public final ForgeConfigSpec.IntValue microscopeProcessTimeTicks;

        public final ForgeConfigSpec.DoubleValue strongLatexHitCoverage;
        public final ForgeConfigSpec.DoubleValue weakLatexHitCoverage;
        public final ForgeConfigSpec.DoubleValue transfurProgressCoverageMultiplier;
        public final ForgeConfigSpec.DoubleValue absorptionCoverageBonus;
        public final ForgeConfigSpec.DoubleValue grabCoverageMultiplier;
        public final ForgeConfigSpec.DoubleValue minimumGrabCoverage;
        public final ForgeConfigSpec.DoubleValue fallbackLatexHitCoverageMultiplier;
        public final ForgeConfigSpec.DoubleValue minimumFallbackLatexHitCoverage;
        public final ForgeConfigSpec.DoubleValue immediateHazardCoverage;

        public final ForgeConfigSpec.IntValue latexNeedIntervalTicks;
        public final ForgeConfigSpec.DoubleValue stabilizedLucidityDrain;
        public final ForgeConfigSpec.DoubleValue unstableLucidityDrain;
        public final ForgeConfigSpec.BooleanValue creativeModeLucidityDrain;
        public final ForgeConfigSpec.DoubleValue maxUnstableLucidityMultiplier;
        public final ForgeConfigSpec.IntValue unstableTicksForMaxMultiplier;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryPerFoodNutrition;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryPerFoodSaturation;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexSmall;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexMedium;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexLarge;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryAquaticUnderwater;
        public final ForgeConfigSpec.DoubleValue lucidityNearbyLatexMaxDrainReduction;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryFromLatexNestSleep;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryFromAssimilation;
        public final ForgeConfigSpec.DoubleValue culturedStrandNestAttunement;
        public final ForgeConfigSpec.DoubleValue culturedStrandAssimilationAttunement;
        public final ForgeConfigSpec.DoubleValue culturedStrandPassiveAttunement;
        public final ForgeConfigSpec.DoubleValue stabilizationRequiredLucidity;

        public final ForgeConfigSpec.BooleanValue reworkGrabEntityControls;
        public final ForgeConfigSpec.BooleanValue latexMobsAttackDifferentLatexPlayers;
        public final ForgeConfigSpec.BooleanValue enableHunterAI;
        public final ForgeConfigSpec.BooleanValue hunterAIRequiresEntityTag;
        public final ForgeConfigSpec.BooleanValue hunterAITargetsOnlyPlayers;
        public final ForgeConfigSpec.BooleanValue hunterAIDebug;
        public final ForgeConfigSpec.IntValue hunterAITickRate;
        public final ForgeConfigSpec.DoubleValue hunterImprovedSightRange;
        public final ForgeConfigSpec.IntValue hunterMemoryAfterLostSightTicks;
        public final ForgeConfigSpec.IntValue hunterTargetLockTicks;
        public final ForgeConfigSpec.DoubleValue watchedDotThreshold;
        public final ForgeConfigSpec.DoubleValue frontConeDotThreshold;
        public final ForgeConfigSpec.DoubleValue behindDotThreshold;
        public final ForgeConfigSpec.BooleanValue watchedLineOfSightRequired;
        public final ForgeConfigSpec.IntValue lineOfSightCheckIntervalTicks;
        public final ForgeConfigSpec.BooleanValue enableCircleApproach;
        public final ForgeConfigSpec.IntValue circleMinTicks;
        public final ForgeConfigSpec.IntValue circleMaxTicks;
        public final ForgeConfigSpec.DoubleValue circleSpeedMultiplier;
        public final ForgeConfigSpec.DoubleValue circleRadiusMin;
        public final ForgeConfigSpec.DoubleValue circleRadiusMax;
        public final ForgeConfigSpec.BooleanValue enableFlank;
        public final ForgeConfigSpec.DoubleValue flankRadiusMin;
        public final ForgeConfigSpec.DoubleValue flankRadiusMax;
        public final ForgeConfigSpec.DoubleValue flankSpeedMultiplier;
        public final ForgeConfigSpec.IntValue flankRepathIntervalTicks;
        public final ForgeConfigSpec.BooleanValue enableIntercept;
        public final ForgeConfigSpec.DoubleValue playerRunningSpeedThreshold;
        public final ForgeConfigSpec.IntValue interceptPredictionTicks;
        public final ForgeConfigSpec.DoubleValue interceptMaxDistance;
        public final ForgeConfigSpec.IntValue interceptCooldownTicks;
        public final ForgeConfigSpec.DoubleValue interceptSpeedMultiplier;
        public final ForgeConfigSpec.BooleanValue enableFakeRetreat;
        public final ForgeConfigSpec.DoubleValue fakeRetreatChance;
        public final ForgeConfigSpec.DoubleValue fakeRetreatMinHealthPercent;
        public final ForgeConfigSpec.IntValue fakeRetreatDurationTicks;
        public final ForgeConfigSpec.IntValue fakeRetreatCooldownTicks;
        public final ForgeConfigSpec.DoubleValue fakeRetreatSpeedMultiplier;
        public final ForgeConfigSpec.BooleanValue enableRealRetreat;
        public final ForgeConfigSpec.DoubleValue lowHealthRetreatThreshold;
        public final ForgeConfigSpec.IntValue maxRealRetreats;
        public final ForgeConfigSpec.IntValue realRetreatDurationTicks;
        public final ForgeConfigSpec.IntValue realRetreatCooldownTicks;
        public final ForgeConfigSpec.DoubleValue realRetreatDistanceMin;
        public final ForgeConfigSpec.DoubleValue realRetreatDistanceMax;
        public final ForgeConfigSpec.DoubleValue realRetreatDropAggroDistance;
        public final ForgeConfigSpec.IntValue realRetreatForgetTargetTicks;
        public final ForgeConfigSpec.DoubleValue realRetreatSpeedMultiplier;
        public final ForgeConfigSpec.BooleanValue enableDesperationRush;
        public final ForgeConfigSpec.IntValue desperationAfterRetreats;
        public final ForgeConfigSpec.DoubleValue desperationSpeedMultiplier;
        public final ForgeConfigSpec.DoubleValue directCommitDistance;
        public final ForgeConfigSpec.IntValue directCommitAfterTicks;
        public final ForgeConfigSpec.IntValue directCommitDurationTicks;
        public final ForgeConfigSpec.DoubleValue directCommitSpeedMultiplier;
        public final ForgeConfigSpec.BooleanValue attackAngleRequirementEnabled;
        public final ForgeConfigSpec.IntValue closeAttackCommitTicks;
        public final ForgeConfigSpec.IntValue attackCooldownTicks;
        public final ForgeConfigSpec.IntValue recoverTicks;
        public final ForgeConfigSpec.BooleanValue enableHunterBehindGrab;
        public final ForgeConfigSpec.BooleanValue enableHunterCloseGrab;
        public final ForgeConfigSpec.DoubleValue hunterCloseGrabChance;
        public final ForgeConfigSpec.IntValue behindGrabRequiredTicks;
        public final ForgeConfigSpec.IntValue behindGrabCooldownTicks;
        public final ForgeConfigSpec.DoubleValue behindGrabMaxDistance;
        public final ForgeConfigSpec.IntValue hunterGrabExposureIntervalTicks;
        public final ForgeConfigSpec.IntValue hunterGrabMaxHoldTicks;
        public final ForgeConfigSpec.IntValue maxHunterAIMobsPerTick;
        public final ForgeConfigSpec.IntValue candidatePointLimit;
        public final ForgeConfigSpec.IntValue pathCheckIntervalTicks;
        public final ForgeConfigSpec.BooleanValue changedLatexMobsIgnoreNaturalSpawnLight;
        public final ForgeConfigSpec.DoubleValue changedLatexMobsDaylightSpawnChance;
        public final ForgeConfigSpec.BooleanValue changedLatexMobsDaylightSpawnIncludesAddons;
        public final ForgeConfigSpec.IntValue changedLatexMobsNaturalSpawnLocalCap;
        public final ForgeConfigSpec.IntValue changedLatexMobsNaturalSpawnLocalCapRadius;
        public final ForgeConfigSpec.BooleanValue debugLatexHeartSpawnMessages;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("coverage");
            waterCoverageWashPerTick = builder.comment("Coverage removed per tick while the player is in water.").defineInRange("waterCoverageWashPerTick", 0.15D, 0.0D, 100.0D);
            rainCoverageWashPerTick = builder.comment("Coverage removed per tick while the player is in rain or bubbles.").defineInRange("rainCoverageWashPerTick", 0.1D, 0.0D, 100.0D);
            passiveCoverageDecayIntervalTicks = builder.comment("How often passive coverage decay runs. 20 ticks = 1 second.").defineInRange("passiveCoverageDecayIntervalTicks", 40, 1, 20 * 60 * 10);
            passiveCoverageDecayAmount = builder.comment("Coverage removed on each passive decay tick.").defineInRange("passiveCoverageDecayAmount", 0.05D, 0.0D, 100.0D);
            coverageInfectionThreshold = builder.comment("Coverage required to begin infection. Default is 100: fully coated means infected.").defineInRange("coverageInfectionThreshold", 100.0D, 1.0D, 100.0D);
            infectionStartPercent = builder.comment("Infection percent assigned when coverage reaches the infection threshold.").defineInRange("infectionStartPercent", 1.0D, 0.0D, 100.0D);
            builder.pop();

            builder.push("infection");
            infectionGrowthIntervalTicks = builder.comment("How often infection grows after it has started. 20 ticks = 1 second.").defineInRange("infectionGrowthIntervalTicks", 40, 1, 20 * 60 * 10);
            infectionGrowthPerInterval = builder.comment("Infection gained each growth interval while not suppressed.").defineInRange("infectionGrowthPerInterval", 0.2D, 0.0D, 100.0D);
            infectionFromExtraCoverageMultiplier = builder.comment("When already infected, extra exposure is converted directly into infection using this multiplier.").defineInRange("infectionFromExtraCoverageMultiplier", 0.45D, 0.0D, 100.0D);
            builder.pop();

            builder.push("treatment");
            disinfectantWipeCoverageRemoval = builder.comment("Coverage removed by one disinfectant wipe.").defineInRange("disinfectantWipeCoverageRemoval", 5.0D, 0.0D, 100.0D);
            disinfectantSprayCoverageRemoval = builder.comment("Coverage removed by one disinfectant spray.").defineInRange("disinfectantSprayCoverageRemoval", 10.0D, 0.0D, 100.0D);
            inhibitorTicks = builder.comment("Ticks of infection suppression granted by latex inhibitor. 20 ticks = 1 second.").defineInRange("inhibitorTicks", 6000, 0, 20 * 60 * 60);
            inhibitorInfectionRemoval = builder.comment("Infection removed by latex inhibitor.").defineInRange("inhibitorInfectionRemoval", 3.0D, 0.0D, 100.0D);
            sampleExtractionDamage = builder.comment("Damage dealt by extracting an infected human sample with the sampling scalpel.").defineInRange("sampleExtractionDamage", 6.0D, 0.0D, 100.0D);
            sampleExtractionMinHealth = builder.comment("Sample extraction refuses to reduce the player below this health.").defineInRange("sampleExtractionMinHealth", 2.0D, 0.0D, 100.0D);
            sampleExtractionInfectionRemoval = builder.comment("Infection removed by cutting out a raw latex sample.").defineInRange("sampleExtractionInfectionRemoval", 2.0D, 0.0D, 100.0D);
            cureDoseInfectionRemoval = builder.comment("Infection removed by consuming one matching strand cure dose.").defineInRange("cureDoseInfectionRemoval", 100.0D, 0.0D, 100.0D);
            cureDoseSuppressantTicks = builder.comment("Ticks of infection suppression granted by one matching strand cure dose.").defineInRange("cureDoseSuppressantTicks", 1200, 0, 20 * 60 * 60);
            builder.pop();

            builder.push("machines");
            microscopeProcessTimeTicks = builder.comment("Ticks needed for a microscope to identify one latex sample. 20 ticks = 1 second.").defineInRange("microscopeProcessTimeTicks", 900, 1, 20 * 60 * 60);
            builder.pop();

            builder.push("exposure");
            strongLatexHitCoverage = builder.comment("Coverage added by a strong Changed latex hit.").defineInRange("strongLatexHitCoverage", 8.0D, 0.0D, 100.0D);
            weakLatexHitCoverage = builder.comment("Coverage added by a weak Changed latex hit.").defineInRange("weakLatexHitCoverage", 4.0D, 0.0D, 100.0D);
            transfurProgressCoverageMultiplier = builder.comment("Coverage added per point of Changed transfur progress requested by the source.").defineInRange("transfurProgressCoverageMultiplier", 1.25D, 0.0D, 100.0D);
            absorptionCoverageBonus = builder.comment("Extra coverage for absorption-style latex attacks.").defineInRange("absorptionCoverageBonus", 3.0D, 0.0D, 100.0D);
            grabCoverageMultiplier = builder.comment("Multiplier for passive coverage from Changed grab progression.").defineInRange("grabCoverageMultiplier", 0.08D, 0.0D, 100.0D);
            minimumGrabCoverage = builder.comment("Minimum coverage applied by grab progression ticks.").defineInRange("minimumGrabCoverage", 0.35D, 0.0D, 100.0D);
            fallbackLatexHitCoverageMultiplier = builder.comment("Coverage added from generic latex hurt fallback per damage point.").defineInRange("fallbackLatexHitCoverageMultiplier", 1.5D, 0.0D, 100.0D);
            minimumFallbackLatexHitCoverage = builder.comment("Minimum coverage added by generic latex hurt fallback.").defineInRange("minimumFallbackLatexHitCoverage", 2.0D, 0.0D, 100.0D);
            immediateHazardCoverage = builder.comment("Coverage added by immediate Changed hazards such as syringes, flasks, puddles, and latex food.").defineInRange("immediateHazardCoverage", 15.0D, 0.0D, 100.0D);
            builder.pop();

            builder.push("latexNeeds");
            latexNeedIntervalTicks = builder.comment("How often lucidity checks tick. 20 ticks = 1 second.").defineInRange("latexNeedIntervalTicks", 40, 1, 20 * 60 * 10);
            stabilizedLucidityDrain = builder.comment("Lucidity drain per interval while stabilized.").defineInRange("stabilizedLucidityDrain", 0.0D, 0.0D, 100.0D);
            unstableLucidityDrain = builder.comment("Base lucidity drain per interval while unstable.").defineInRange("unstableLucidityDrain", 0.1D, 0.0D, 100.0D);
            creativeModeLucidityDrain = builder.comment("If true, creative-mode latex players still lose lucidity. If false, creative mode freezes lucidity drain.").define("creativeModeLucidityDrain", false);
            maxUnstableLucidityMultiplier = builder.comment("Lucidity drains faster as instability increases. This is the maximum multiplier.").defineInRange("maxUnstableLucidityMultiplier", 7.5D, 1.0D, 100.0D);
            unstableTicksForMaxMultiplier = builder.comment("How many ticks does it take for lucidity to reach the maximum drain multiplier.").defineInRange("unstableTicksForMaxMultiplier", 24000, 1, 20 * 60 * 60 * 24);
            lucidityRecoveryPerFoodNutrition = builder.comment("Lucidity restored when a latex player finishes eating food, per nutrition point.").defineInRange("lucidityRecoveryPerFoodNutrition", 0.4D, 0.0D, 100.0D);
            lucidityRecoveryPerFoodSaturation = builder.comment("Lucidity restored when a latex player finishes eating food, per saturation modifier point.").defineInRange("lucidityRecoveryPerFoodSaturation", 3.0D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexSmall = builder.comment("Lucidity drain offset per latex need interval near a small amount of friendly latex.").defineInRange("lucidityRecoveryNearLatexSmall", 0.3D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexMedium = builder.comment("Lucidity drain offset per latex need interval near a moderate amount of friendly latex.").defineInRange("lucidityRecoveryNearLatexMedium", 0.5D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexLarge = builder.comment("Lucidity drain offset per latex need interval near a dense friendly latex nest.").defineInRange("lucidityRecoveryNearLatexLarge", 0.75D, 0.0D, 100.0D);
            lucidityRecoveryAquaticUnderwater = builder.comment("Lucidity drain offset per latex need interval while an aquatic latex form is underwater.").defineInRange("lucidityRecoveryAquaticUnderwater", 0.5D, 0.0D, 100.0D);
            lucidityNearbyLatexMaxDrainReduction = builder.comment("Maximum fraction of normal lucidity drain that nearby friendly latex can cancel. 0.75 means at least 25% of drain still applies.").defineInRange("lucidityNearbyLatexMaxDrainReduction", 0.75D, 0.0D, 1.0D);
            lucidityRecoveryFromLatexNestSleep = builder.comment("Lucidity restored when waking after sleeping long enough in a friendly latex nest.").defineInRange("lucidityRecoveryFromLatexNestSleep", 25.0D, 0.0D, 100.0D);
            lucidityRecoveryFromAssimilation = builder.comment("Lucidity restored when a latex player successfully assimilates/transfurs another entity.").defineInRange("lucidityRecoveryFromAssimilation", 20.0D, 0.0D, 100.0D);
            culturedStrandNestAttunement = builder.comment("Attunement added to a carried matching cultured strand when sleeping in a friendly latex nest.").defineInRange("culturedStrandNestAttunement", 40.0D, 0.0D, 100.0D);
            culturedStrandAssimilationAttunement = builder.comment("Attunement added to a carried matching cultured strand after successful assimilation.").defineInRange("culturedStrandAssimilationAttunement", 25.0D, 0.0D, 100.0D);
            culturedStrandPassiveAttunement = builder.comment("Attunement added once per minute to a carried matching cultured strand while lucid near friendly latex.").defineInRange("culturedStrandPassiveAttunement", 1.0D, 0.0D, 100.0D);
            stabilizationRequiredLucidity = builder.comment("Minimum lucidity required to consume a stabilization dose.").defineInRange("stabilizationRequiredLucidity", 80.0D, 0.0D, 100.0D);
            builder.pop();

            builder.push("misc");
            reworkGrabEntityControls = builder.comment("If true, Changed Grab Entity controls are reworked: left click absorbs/assimilates, right click replicates.").define("reworkGrabEntityControls", true);
            latexMobsAttackDifferentLatexPlayers = builder.comment("If true, Changed latex mobs can target transfurred players unless they share/friend the same latex type.").define("latexMobsAttackDifferentLatexPlayers", true);
            builder.push("hunterAI");
            enableHunterAI = builder.comment("If true, eligible Changed latex beasts can use the Survive Protocol solo hunter AI.").define("enableHunterAI", true);
            hunterAIRequiresEntityTag = builder.comment("If true, hunter AI only applies to entity types in the changed_survive_protocol:hunter_ai_latex_beasts tag.").define("hunterAIRequiresEntityTag", true);
            hunterAITargetsOnlyPlayers = builder.comment("Keep true. The current hunter AI is intentionally designed around player targets only.").define("hunterAITargetsOnlyPlayers", true);
            hunterAIDebug = builder.comment("If true, logs hunter AI state changes for eligible mobs.").define("hunterAIDebug", false);
            hunterAITickRate = builder.comment("How often the hunter brain reevaluates non-urgent decisions.").defineInRange("hunterAITickRate", 4, 1, 40);
            hunterImprovedSightRange = builder.comment("Maximum distance hunter AI will keep a tracked target active.").defineInRange("hunterImprovedSightRange", 64.0D, 4.0D, 128.0D);
            hunterMemoryAfterLostSightTicks = builder.comment("How long hunter AI remembers a target's last seen position after line of sight is lost.").defineInRange("hunterMemoryAfterLostSightTicks", 360, 1, 20 * 60);
            hunterTargetLockTicks = builder.comment("How long a hunter prefers to keep its current target before switching.").defineInRange("hunterTargetLockTicks", 320, 1, 20 * 60);
            watchedDotThreshold = builder.comment("Dot-product threshold for deciding that the target is staring directly at the hunter.").defineInRange("watchedDotThreshold", 0.72D, -1.0D, 1.0D);
            frontConeDotThreshold = builder.comment("Dot-product threshold for deciding that the hunter is in the target's front cone.").defineInRange("frontConeDotThreshold", 0.2D, -1.0D, 1.0D);
            behindDotThreshold = builder.comment("Dot-product threshold for deciding that the hunter is behind the target.").defineInRange("behindDotThreshold", -0.3D, -1.0D, 1.0D);
            watchedLineOfSightRequired = builder.comment("If true, watched detection only counts when the target also has line of sight to the hunter.").define("watchedLineOfSightRequired", true);
            lineOfSightCheckIntervalTicks = builder.comment("How often hunter AI refreshes cached line-of-sight checks.").defineInRange("lineOfSightCheckIntervalTicks", 6, 1, 40);
            enableCircleApproach = builder.comment("If true, watched hunters can circle while closing in.").define("enableCircleApproach", true);
            circleMinTicks = builder.comment("Minimum duration of circle-approach behavior.").defineInRange("circleMinTicks", 18, 1, 20 * 30);
            circleMaxTicks = builder.comment("Maximum duration of circle-approach behavior.").defineInRange("circleMaxTicks", 70, 1, 20 * 30);
            circleSpeedMultiplier = builder.comment("Navigation speed multiplier used while circling. Changed's normal melee AI is roughly 0.4.").defineInRange("circleSpeedMultiplier", 0.30D, 0.05D, 2.0D);
            circleRadiusMin = builder.comment("Minimum orbit radius used by circle-approach.").defineInRange("circleRadiusMin", 3.5D, 0.5D, 32.0D);
            circleRadiusMax = builder.comment("Maximum orbit radius used by circle-approach.").defineInRange("circleRadiusMax", 7.0D, 1.0D, 32.0D);
            enableFlank = builder.comment("If true, hunters prefer side and rear approach points while closing in.").define("enableFlank", true);
            flankRadiusMin = builder.comment("Minimum distance used for flank candidate points.").defineInRange("flankRadiusMin", 2.5D, 0.5D, 32.0D);
            flankRadiusMax = builder.comment("Maximum distance used for flank candidate points.").defineInRange("flankRadiusMax", 6.5D, 1.0D, 32.0D);
            flankSpeedMultiplier = builder.comment("Navigation speed multiplier used while flanking. Changed's normal melee AI is roughly 0.4.").defineInRange("flankSpeedMultiplier", 0.34D, 0.05D, 2.0D);
            flankRepathIntervalTicks = builder.comment("How often flank and circle states recompute movement targets.").defineInRange("flankRepathIntervalTicks", 12, 1, 40);
            enableIntercept = builder.comment("If true, hunters may cut off players running in a straight line.").define("enableIntercept", true);
            playerRunningSpeedThreshold = builder.comment("Minimum horizontal target speed required before intercept behavior can trigger.").defineInRange("playerRunningSpeedThreshold", 0.14D, 0.01D, 2.0D);
            interceptPredictionTicks = builder.comment("How far ahead hunter AI predicts a running target.").defineInRange("interceptPredictionTicks", 10, 1, 40);
            interceptMaxDistance = builder.comment("Maximum distance ahead hunter AI will predict for intercept.").defineInRange("interceptMaxDistance", 6.0D, 1.0D, 32.0D);
            interceptCooldownTicks = builder.comment("Cooldown between intercept attempts.").defineInRange("interceptCooldownTicks", 50, 0, 20 * 60);
            interceptSpeedMultiplier = builder.comment("Navigation speed multiplier used while intercepting. Changed's normal melee AI is roughly 0.4.").defineInRange("interceptSpeedMultiplier", 0.40D, 0.05D, 2.0D);
            enableFakeRetreat = builder.comment("If true, hunters can briefly back off to bait the player into making a mistake.").define("enableFakeRetreat", true);
            fakeRetreatChance = builder.comment("Base chance per fake-retreat decision roll when the target is actively pushing into the hunter.").defineInRange("fakeRetreatChance", 0.06D, 0.0D, 1.0D);
            fakeRetreatMinHealthPercent = builder.comment("Minimum health ratio required before fake retreat is allowed. Below this, real retreat or desperation should take over.").defineInRange("fakeRetreatMinHealthPercent", 0.45D, 0.01D, 1.0D);
            fakeRetreatDurationTicks = builder.comment("How long a fake retreat lasts before the hunter recommits.").defineInRange("fakeRetreatDurationTicks", 28, 1, 20 * 30);
            fakeRetreatCooldownTicks = builder.comment("Cooldown between fake retreat attempts.").defineInRange("fakeRetreatCooldownTicks", 240, 0, 20 * 60);
            fakeRetreatSpeedMultiplier = builder.comment("Navigation speed multiplier used while fake-retreating. Changed's normal melee AI is roughly 0.4.").defineInRange("fakeRetreatSpeedMultiplier", 0.36D, 0.05D, 2.0D);
            enableRealRetreat = builder.comment("If true, wounded hunters can briefly disengage before returning.").define("enableRealRetreat", true);
            lowHealthRetreatThreshold = builder.comment("Health ratio at or below which hunter AI may enter a real retreat.").defineInRange("lowHealthRetreatThreshold", 0.35D, 0.01D, 1.0D);
            maxRealRetreats = builder.comment("Maximum number of real retreats a hunter can use before desperation begins.").defineInRange("maxRealRetreats", 2, 0, 10);
            realRetreatDurationTicks = builder.comment("Maximum time a real retreat may spend running before it drops aggro anyway.").defineInRange("realRetreatDurationTicks", 120, 1, 20 * 30);
            realRetreatCooldownTicks = builder.comment("Cooldown between real retreats.").defineInRange("realRetreatCooldownTicks", 140, 0, 20 * 60);
            realRetreatDistanceMin = builder.comment("Minimum distance a real retreat tries to put between the hunter and target.").defineInRange("realRetreatDistanceMin", 18.0D, 4.0D, 96.0D);
            realRetreatDistanceMax = builder.comment("Maximum distance a real retreat tries to put between the hunter and target.").defineInRange("realRetreatDistanceMax", 30.0D, 4.0D, 128.0D);
            realRetreatDropAggroDistance = builder.comment("Once this far from the target during a real retreat, the hunter drops aggro.").defineInRange("realRetreatDropAggroDistance", 22.0D, 4.0D, 128.0D);
            realRetreatForgetTargetTicks = builder.comment("After a real retreat drops aggro, ignore the same target for this long. 20 ticks = 1 second.").defineInRange("realRetreatForgetTargetTicks", 300, 0, 20 * 120);
            realRetreatSpeedMultiplier = builder.comment("Navigation speed multiplier used while retreating. Changed's normal melee AI is roughly 0.4.").defineInRange("realRetreatSpeedMultiplier", 0.42D, 0.05D, 2.0D);
            enableDesperationRush = builder.comment("If true, hunters become more direct after exhausting their real retreats.").define("enableDesperationRush", true);
            desperationAfterRetreats = builder.comment("Number of real retreats after which desperation is enabled.").defineInRange("desperationAfterRetreats", 2, 0, 10);
            desperationSpeedMultiplier = builder.comment("Navigation speed multiplier used during desperation rush. Changed's normal melee AI is roughly 0.4.").defineInRange("desperationSpeedMultiplier", 0.45D, 0.05D, 2.0D);
            directCommitDistance = builder.comment("Visible hunters within this horizontal distance may stop fishing for a perfect flank and rush directly.").defineInRange("directCommitDistance", 5.5D, 1.0D, 24.0D);
            directCommitAfterTicks = builder.comment("How long a visible close hunter may circle/flank before it temporarily commits directly. 20 ticks = 1 second.").defineInRange("directCommitAfterTicks", 26, 0, 20 * 30);
            directCommitDurationTicks = builder.comment("Maximum duration of one direct commit burst. 20 ticks = 1 second.").defineInRange("directCommitDurationTicks", 18, 1, 20 * 10);
            directCommitSpeedMultiplier = builder.comment("Navigation speed multiplier used during direct commit bursts. Changed's normal melee AI is roughly 0.4.").defineInRange("directCommitSpeedMultiplier", 0.48D, 0.05D, 2.0D);
            attackAngleRequirementEnabled = builder.comment("If true, hunters avoid front-facing attacks unless desperate.").define("attackAngleRequirementEnabled", true);
            closeAttackCommitTicks = builder.comment("How long a hunter already in hit range may wait for a better angle before attacking anyway. Set to 0 for immediate close attacks.").defineInRange("closeAttackCommitTicks", 4, 0, 20 * 10);
            attackCooldownTicks = builder.comment("Cooldown between hunter attack attempts.").defineInRange("attackCooldownTicks", 18, 0, 20 * 60);
            recoverTicks = builder.comment("Recovery time after an attack attempt.").defineInRange("recoverTicks", 12, 0, 20 * 30);
            enableHunterBehindGrab = builder.comment("If true, hunters with Changed's grab ability may grab when they earn a close rear attack angle.").define("enableHunterBehindGrab", true);
            enableHunterCloseGrab = builder.comment("If true, hunters with Changed's grab ability may sometimes grab instead of hit during close commit attacks.").define("enableHunterCloseGrab", true);
            hunterCloseGrabChance = builder.comment("Chance that a close commit attack becomes a grab instead of a normal hit when contact checks pass.").defineInRange("hunterCloseGrabChance", 0.35D, 0.0D, 1.0D);
            behindGrabRequiredTicks = builder.comment("How long the hunter must stay behind the target before a grab can replace a normal attack.").defineInRange("behindGrabRequiredTicks", 5, 0, 20 * 10);
            behindGrabCooldownTicks = builder.comment("Cooldown between successful hunter grabs.").defineInRange("behindGrabCooldownTicks", 100, 0, 20 * 60);
            behindGrabMaxDistance = builder.comment("Maximum horizontal distance for hunter grabs. The hitboxes must still be close enough to touch.").defineInRange("behindGrabMaxDistance", 1.8D, 0.5D, 6.0D);
            hunterGrabExposureIntervalTicks = builder.comment("How often a hunter grab applies coverage while holding a human player. Set to 0 to disable passive hunter-grab exposure.").defineInRange("hunterGrabExposureIntervalTicks", 10, 0, 20 * 10);
            hunterGrabMaxHoldTicks = builder.comment("Maximum duration of a hunter grab before it releases automatically. Set to 0 to let Changed's normal escape rules decide.").defineInRange("hunterGrabMaxHoldTicks", 100, 0, 20 * 60);
            maxHunterAIMobsPerTick = builder.comment("Maximum hunter mobs allowed to start a new movement/path update per tick. Set to 0 to disable the budget.").defineInRange("maxHunterAIMobsPerTick", 24, 0, 512);
            candidatePointLimit = builder.comment("Maximum number of path candidates evaluated when choosing a movement point.").defineInRange("candidatePointLimit", 6, 1, 32);
            pathCheckIntervalTicks = builder.comment("Minimum interval between general path recalculations.").defineInRange("pathCheckIntervalTicks", 10, 1, 40);
            builder.pop();
            changedLatexMobsIgnoreNaturalSpawnLight = builder.comment("If true, natural Changed latex mob spawns can ignore the vanilla monster light check, allowing limited daytime or lit-area spawns while keeping biome, ground, difficulty, collision, and local cap rules.").define("changedLatexMobsIgnoreNaturalSpawnLight", false);
            changedLatexMobsDaylightSpawnChance = builder.comment("Chance for a failed natural Changed latex spawn to be retried while ignoring light. Lower values keep daytime spawns rare.").defineInRange("changedLatexMobsDaylightSpawnChance", 0.25D, 0.0D, 1.0D);
            changedLatexMobsDaylightSpawnIncludesAddons = builder.comment("If true, the daylight spawn bypass also affects addon latex entities. Keep false if addon mobs overpopulate broad biome tags.").define("changedLatexMobsDaylightSpawnIncludesAddons", false);
            changedLatexMobsNaturalSpawnLocalCap = builder.comment("Maximum nearby latex mobs before new natural latex spawns are denied. Set to 0 to disable this cap.").defineInRange("changedLatexMobsNaturalSpawnLocalCap", 8, 0, 200);
            changedLatexMobsNaturalSpawnLocalCapRadius = builder.comment("Radius checked by changedLatexMobsNaturalSpawnLocalCap.").defineInRange("changedLatexMobsNaturalSpawnLocalCapRadius", 48, 1, 256);
            builder.pop();

            builder.push("debug");
            debugLatexHeartSpawnMessages = builder.comment("If true, sends a clickable chat message whenever a latex heart successfully spawns.").define("latexHeartSpawnMessages", false);
            builder.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue infectionOverlayEnabled;
        public final ForgeConfigSpec.DoubleValue infectionOverlayLowEndPercent;
        public final ForgeConfigSpec.DoubleValue infectionOverlayCriticalStartPercent;
        public final ForgeConfigSpec.DoubleValue infectionOverlayLowMaxAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayMidMaxAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayCriticalAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayPulseAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayFinalWashAlpha;

        public final ForgeConfigSpec.BooleanValue sharedStatusMeterEnabled;
        public final ForgeConfigSpec.DoubleValue coverageVignetteMaxAlpha;
        public final ForgeConfigSpec.DoubleValue lowLucidityVignetteMaxAlpha;

        public final ForgeConfigSpec.BooleanValue darkLatexMaskOverlayEnabled;
        public final ForgeConfigSpec.DoubleValue darkLatexMaskOverlayAlpha;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("infectionOverlay");
            infectionOverlayEnabled = builder.define("infectionOverlayEnabled", true);
            infectionOverlayLowEndPercent = builder.comment("At this infection percent, the goo outline becomes clearly visible.").defineInRange("visibleAtPercent", 25.0D, 0.0D, 100.0D);
            infectionOverlayCriticalStartPercent = builder.comment("At this infection percent, the goo outline is nearly full and the final color wash begins.").defineInRange("finalWashStartPercent", 90.0D, 0.0D, 100.0D);
            infectionOverlayLowMaxAlpha = builder.comment("Goo outline alpha at visibleAtPercent.").defineInRange("visibleAlpha", 0.20D, 0.0D, 1.0D);
            infectionOverlayMidMaxAlpha = builder.comment("Goo outline alpha just before finalWashStartPercent.").defineInRange("midAlpha", 0.82D, 0.0D, 1.0D);
            infectionOverlayCriticalAlpha = builder.comment("Goo outline alpha from finalWashStartPercent to full infection.").defineInRange("criticalAlpha", 0.95D, 0.0D, 1.0D);
            infectionOverlayPulseAlpha = builder.comment("Extra pulsing alpha added as infection becomes severe.").defineInRange("pulseAlpha", 0.04D, 0.0D, 1.0D);
            infectionOverlayFinalWashAlpha = builder.comment("Maximum screen wash during the final infection surge.").defineInRange("finalWhiteWashAlpha", 0.88D, 0.0D, 1.0D);
            builder.pop();

            builder.push("meters");
            sharedStatusMeterEnabled = builder.comment("If true, one Changed-style meter shows coverage before infection, infection while infected, and lucidity while latex.").define("sharedStatusMeterEnabled", true);
            coverageVignetteMaxAlpha = builder.comment("Maximum coverage vignette alpha while human. Keep this low; high values are rough on the eyes.").defineInRange("coverageVignetteMaxAlpha", 0.16D, 0.0D, 1.0D);
            lowLucidityVignetteMaxAlpha = builder.comment("Maximum vignette alpha as latex lucidity gets dangerously low.").defineInRange("lowLucidityVignetteMaxAlpha", 0.24D, 0.0D, 1.0D);
            builder.pop();

            builder.push("darkLatexMaskOverlay");
            darkLatexMaskOverlayEnabled = builder.comment("If true, dark-latex players may show the built-in latex mask overlay.").define("enabled", false);
            darkLatexMaskOverlayAlpha = builder.comment("Mask overlay alpha.").defineInRange("alpha", 0.65D, 0.0D, 1.0D);
            builder.pop();
        }
    }
}
