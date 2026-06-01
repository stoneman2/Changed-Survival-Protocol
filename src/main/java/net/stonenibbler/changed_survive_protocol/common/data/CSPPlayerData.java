package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class CSPPlayerData {
    private double infectionPercent;
    private double coverage;
    private boolean infected;
    private String strainId = "";
    private int suppressantTicks;
    private double lucidity = 100.0D;
    private boolean lucidityActive;
    private boolean unstableLatex;
    private boolean stabilizedLatex;
    private int unstableLatexTicks;
    private double lucidityDrainMultiplier = 1.0D;
    private String settledStrainId = "";
    private UUID feralSelfUuid;
    private int collapseCount;

    private long lastExposureGameTime = Long.MIN_VALUE;
    private UUID lastExposureSource;

    public void copyFrom(CSPPlayerData other) {
        infectionPercent = other.infectionPercent;
        coverage = other.coverage;
        infected = other.infected;
        strainId = other.strainId;
        suppressantTicks = other.suppressantTicks;
        lucidity = other.lucidity;
        lucidityActive = other.lucidityActive;
        unstableLatex = other.unstableLatex;
        stabilizedLatex = other.stabilizedLatex;
        unstableLatexTicks = other.unstableLatexTicks;
        lucidityDrainMultiplier = other.lucidityDrainMultiplier;
        settledStrainId = other.settledStrainId;
        feralSelfUuid = other.feralSelfUuid;
        collapseCount = other.collapseCount;
    }

    public void reset() {
        infectionPercent = 0.0D;
        coverage = 0.0D;
        infected = false;
        strainId = "";
        suppressantTicks = 0;
        lucidity = 100.0D;
        lucidityActive = false;
        unstableLatex = false;
        stabilizedLatex = false;
        unstableLatexTicks = 0;
        lucidityDrainMultiplier = 1.0D;
        settledStrainId = "";
        feralSelfUuid = null;
        collapseCount = 0;
        lastExposureGameTime = Long.MIN_VALUE;
        lastExposureSource = null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("infectionPercent", infectionPercent);
        tag.putDouble("coverage", coverage);
        tag.putBoolean("infected", infected);
        tag.putString("strainId", strainId);
        tag.putInt("suppressantTicks", suppressantTicks);
        tag.putDouble("lucidity", lucidity);
        tag.putBoolean("lucidityActive", lucidityActive);
        tag.putBoolean("unstableLatex", unstableLatex);
        tag.putBoolean("stabilizedLatex", stabilizedLatex);
        tag.putInt("unstableLatexTicks", unstableLatexTicks);
        tag.putDouble("lucidityDrainMultiplier", lucidityDrainMultiplier);
        tag.putString("settledStrainId", settledStrainId);
        if (feralSelfUuid != null) {
            tag.putUUID("feralSelfUuid", feralSelfUuid);
        }
        tag.putInt("collapseCount", collapseCount);
        return tag;
    }

    public void load(CompoundTag tag) {
        infectionPercent = clampPercent(tag.getDouble("infectionPercent"));
        coverage = clampPercent(tag.getDouble("coverage"));
        infected = tag.getBoolean("infected");
        strainId = tag.getString("strainId");
        suppressantTicks = Math.max(0, tag.getInt("suppressantTicks"));
        lucidity = tag.contains("lucidity") ? clampPercent(tag.getDouble("lucidity")) : 100.0D;
        lucidityActive = tag.getBoolean("lucidityActive");
        unstableLatex = tag.getBoolean("unstableLatex");
        stabilizedLatex = tag.getBoolean("stabilizedLatex");
        unstableLatexTicks = Math.max(0, tag.getInt("unstableLatexTicks"));
        lucidityDrainMultiplier = tag.contains("lucidityDrainMultiplier") ? Math.max(0.0D, tag.getDouble("lucidityDrainMultiplier")) : 1.0D;
        settledStrainId = tag.getString("settledStrainId");
        feralSelfUuid = tag.hasUUID("feralSelfUuid") ? tag.getUUID("feralSelfUuid") : null;
        collapseCount = Math.max(0, tag.getInt("collapseCount"));
    }

    public double getInfectionPercent() {
        return infectionPercent;
    }

    public void setInfectionPercent(double value) {
        infectionPercent = clampPercent(value);
        infected = infectionPercent > 0.0D;
    }

    public void clearInfection() {
        infectionPercent = 0.0D;
        infected = false;
        strainId = "";
        suppressantTicks = 0;
    }

    public void addInfection(double amount) {
        setInfectionPercent(infectionPercent + amount);
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double value) {
        coverage = clampPercent(value);
    }

    public void clearCoverage() {
        coverage = 0.0D;
    }

    public void addCoverage(double amount) {
        setCoverage(coverage + amount);
    }

    public boolean isInfected() {
        return infected;
    }

    public void setInfected(boolean infected) {
        this.infected = infected;
        if (!infected) {
            infectionPercent = 0.0D;
        } else if (infectionPercent <= 0.0D) {
            infectionPercent = 1.0D;
        }
    }

    public String getStrainId() {
        return strainId;
    }

    public void setStrainId(String strainId) {
        this.strainId = strainId == null ? "" : strainId;
    }

    public int getSuppressantTicks() {
        return suppressantTicks;
    }

    public void setSuppressantTicks(int suppressantTicks) {
        this.suppressantTicks = Math.max(0, suppressantTicks);
    }

    public boolean tickSuppressant() {
        if (suppressantTicks <= 0) {
            return false;
        }
        suppressantTicks--;
        return true;
    }

    public double getLucidity() {
        return lucidity;
    }

    public void setLucidity(double value) {
        lucidity = clampPercent(value);
    }

    public void addLucidity(double amount) {
        setLucidity(lucidity + amount);
    }

    public boolean isLucidityActive() {
        return lucidityActive;
    }

    public void setLucidityActive(boolean lucidityActive) {
        this.lucidityActive = lucidityActive;
    }

    public boolean isUnstableLatex() {
        return unstableLatex;
    }

    public void setUnstableLatex(boolean unstableLatex) {
        this.unstableLatex = unstableLatex;
    }

    public boolean isStabilizedLatex() {
        return stabilizedLatex;
    }

    public void setStabilizedLatex(boolean stabilizedLatex) {
        this.stabilizedLatex = stabilizedLatex;
    }

    public int getUnstableLatexTicks() {
        return unstableLatexTicks;
    }

    public void setUnstableLatexTicks(int unstableLatexTicks) {
        this.unstableLatexTicks = Math.max(0, unstableLatexTicks);
    }

    public void addUnstableLatexTick() {
        unstableLatexTicks++;
    }

    public double getLucidityDrainMultiplier() {
        return lucidityDrainMultiplier;
    }

    public void setLucidityDrainMultiplier(double lucidityDrainMultiplier) {
        this.lucidityDrainMultiplier = Math.max(0.0D, lucidityDrainMultiplier);
    }

    public String getSettledStrainId() {
        return settledStrainId;
    }

    public void setSettledStrainId(String settledStrainId) {
        this.settledStrainId = settledStrainId == null ? "" : settledStrainId;
    }

    public boolean hasSettledStrain() {
        return !settledStrainId.isBlank();
    }

    public UUID getFeralSelfUuid() {
        return feralSelfUuid;
    }

    public void setFeralSelfUuid(UUID feralSelfUuid) {
        this.feralSelfUuid = feralSelfUuid;
    }

    public int getCollapseCount() {
        return collapseCount;
    }

    public void setCollapseCount(int collapseCount) {
        this.collapseCount = Math.max(0, collapseCount);
    }

    public void incrementCollapseCount() {
        collapseCount++;
    }

    public boolean shouldAcceptExposure(long gameTime, UUID sourceUuid) {
        if (gameTime == lastExposureGameTime && (sourceUuid == null ? lastExposureSource == null : sourceUuid.equals(lastExposureSource))) {
            return false;
        }
        lastExposureGameTime = gameTime;
        lastExposureSource = sourceUuid;
        return true;
    }

    private static double clampPercent(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
