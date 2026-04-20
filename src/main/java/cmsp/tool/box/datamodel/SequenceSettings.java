package cmsp.tool.box.datamodel;

import cmsp.tool.box.enums.SequenceInstrumentTypes;

import static cmsp.tool.box.utils.MathUtils.isInteger;

public class SequenceSettings {

    private String prefix;
    private Boolean targeted;
    private String instrument;
    private String startPosition;
    private Boolean batchInjections;
    private Integer batchNum;
    private Boolean batchMax;
    private String sampleRandomize;
    private String sampleAnalyze;
    private Boolean calRun;
    private Integer calTopLevel;
    private Integer calBtmLevel;
    private String calAnalyze;
    private Boolean libRun;
    private String libSchema;
    private String libAnalyze;
    private Boolean qcRun;
    private Integer qcLevels;
    private String qcAnalyze;
    private Integer qcNum;
    private Boolean sstRun;
    private Integer sstLevels;
    private String sstAnalyze;
    private Integer sstNum;
    private Boolean negRun;
    private Integer negNumBCal;
    private Integer negNumACal;
    private Integer negNumBQc;
    private Integer negNumAQc;
    private Boolean blankRun;
    private Integer blankNumBCal;
    private Integer blankNumACal;
    private Integer blankNumBQc;
    private Integer blankNumAQc;

    private Integer blankStartPosition;
    private Integer negStartPosition;
    private Integer calStartPosition;
    private Integer sstStartPosition;
    private Integer qcStartPosition;
    private Integer sampleStartPosition;


    public SequenceSettings(String prefix, String experimentType, String instrument, String startPosition,
                            String batchType, Integer batchNum, Boolean batchMax, String sampleRandomize,
                            String sampleAnalyze, Boolean cal, Integer calTop, Integer calBottom, String calAnalyze,
                            Boolean lib, String libSchema, String libAnalyze, Boolean qc, Integer qcLevels,
                            String qcAnalyze, Integer qcNum, Boolean sst, Integer sstLevels, String sstAnalyze,
                            Integer sstNum, Boolean neg, Integer negBCal, Integer negACal, Integer negBQc, Integer negAQc,
                            Boolean blank, Integer blankBCal, Integer blankACal, Integer blankBQc, Integer blankAQc) {

        this.prefix = prefix;
        this.targeted = experimentType.equals("Targeted");
        this.instrument = instrument;
        this.startPosition = startPosition;
        this.batchInjections = batchType.equals("Injections");
        this.batchNum = batchNum;
        this.batchMax = batchMax;
        this.sampleRandomize = sampleRandomize;
        this.sampleAnalyze = sampleAnalyze;
        this.calRun = cal;
        this.calTopLevel = calTop;
        this.calBtmLevel = calBottom;
        this.calAnalyze = calAnalyze;
        this.libRun = lib;
        this.libSchema = libSchema;
        this.libAnalyze = libAnalyze;
        this.qcRun = qc;
        this.qcLevels = qcLevels;
        this.qcAnalyze = qcAnalyze;
        this.qcNum = qcNum;
        this.sstRun = sst;
        this.sstLevels = sstLevels;
        this.sstAnalyze = sstAnalyze;
        this.sstNum = sstNum;
        this.negRun = neg;
        this.negNumBCal = negBCal;
        this.negNumACal = negACal;
        this.negNumBQc = negBQc;
        this.negNumAQc = negAQc;
        this.blankRun = blank;
        this.blankNumBCal = blankBCal;
        this.blankNumACal = blankACal;
        this.blankNumBQc = blankBQc;
        this.blankNumAQc = blankAQc;

        calculateStartPositions();
    }

    private void calculateStartPositions() {


        int startPosition = (isInteger(this.startPosition)) ? Integer.parseInt(this.startPosition) : convertPositionStringToInt();
        if (blankRun) {
            this.blankStartPosition = startPosition;
            startPosition = startPosition + 1;
        }

        if (negRun) {
            this.negStartPosition = startPosition;
            startPosition = startPosition + 1;
        }

        if (calRun) {
            this.calStartPosition = startPosition;
            startPosition = startPosition + getCalTotalNumber();
        }

        if (libRun) {
            this.calStartPosition = startPosition;
            startPosition = startPosition + getLibPositionNumber();
        }

        if (sstRun) {
            this.sstStartPosition = startPosition;
            startPosition = startPosition + sstLevels;
        }

        if (qcRun) {
            this.qcStartPosition = startPosition;
            startPosition = startPosition + qcLevels;
        }

        this.sampleStartPosition = startPosition;
    }

    private int convertPositionStringToInt() {
        return switch (instrument) {
            case "Agilent 6495C" -> convertAgilentPositionToInt();
            case "Sciex 5500" -> 1;
            case "Sciex 6500" -> 1;
            case "Thermo Eclipse" -> convertThermoPositionToInt();
            case "Thermo Fusion" -> convertThermoPositionToInt();
            case "Thermo QExactive" -> convertThermoPositionToInt();
            default -> 1;
        };
    }

    public String convertIntToPosition(Integer number) {
        return switch (instrument) {
            case "Agilent 6495C" -> convertIntToAgilentPosition(number);
            case "Agilent 7200" -> String.valueOf(number);
            case "Sciex 5500" -> String.valueOf(number);
            case "Sciex 6500" -> String.valueOf(number);
            case "Thermo Eclipse" -> convertIntToThermoPosition(number);
            case "Thermo Fusion" -> convertIntToThermoPosition(number);
            case "Thermo QExactive" -> convertIntToThermoPosition(number);
            default -> String.valueOf(number);
        };
    }

    private String convertIntToThermoPosition(Integer number) {

        String position = "";

        if (number > 80) {
            position = position + "G";
            number = number - 80;
        } else if (number > 40) {
            position = position + "B";
            number = number - 40;
        } else {
            position = position + "R";
        }

        int letter = (number - 1) / 8;
        switch (letter) {
            case 0 -> position = position + "A";
            case 1 -> position = position + "B";
            case 2 -> position = position + "C";
            case 3 -> position = position + "D";
            case 4 -> position = position + "E";
        }

        int num = (number % 8 == 0) ? 8 : number % 8;

        return position + num;
    }

    private String convertIntToAgilentPosition(Integer number) {

        String position = "";

        if (number > 162) {
            position = position + "P4-";
            number = number - 162;
        } else if (number > 108) {
            position = position + "P3-";
            number = number - 108;
        } else if (number > 54) {
            position = position + "P2-";
            number = number - 54;
        } else {
            position = position + "P1-";
        }

        int letter = (number - 1) / 9;
        switch (letter) {
            case 0 -> position = position + "A";
            case 1 -> position = position + "B";
            case 2 -> position = position + "C";
            case 3 -> position = position + "D";
            case 4 -> position = position + "E";
            case 5 -> position = position + "F";
        }

        int num = (number % 9 == 0) ? 9 : number % 9;

        return position + num;
    }

    private int convertThermoPositionToInt() {

        char[] positionArray = startPosition.toCharArray();
        int position = Character.getNumericValue(positionArray[2]);

        switch (positionArray[0]) {
            case 'B' -> position = position + 40;
            case 'G' -> position = position + 80;
        }

        switch (positionArray[1]) {
            case 'B' -> position = position + 8;
            case 'C' -> position = position + 16;
            case 'D' -> position = position + 24;
            case 'E' -> position = position + 32;
        }

        return position;
    }

    private int convertAgilentPositionToInt() {

        char[] positionArray = startPosition.toCharArray();
        int position = Character.getNumericValue(positionArray[4]);

        switch (positionArray[1]) {
            case '2' -> position = position + 54;
            case '3' -> position = position + 108;
            case '4' -> position = position + 162;
        }

        switch (positionArray[3]) {
            case 'B' -> position = position + 9;
            case 'C' -> position = position + 18;
            case 'D' -> position = position + 27;
            case 'E' -> position = position + 36;
            case 'F' -> position = position + 45;
        }

        return position;
    }

    public Integer getBlankNumACal() {
        return blankNumACal;
    }

    public int getBlankNumAQc() {
        return this.blankNumAQc;
    }

    public Integer getBlankNumBCal() {
        return blankNumBCal;
    }

    public Integer getBlankNumBQc() {
        return blankNumBQc;
    }

    public Integer getBlankStartPosition() {
        return blankStartPosition;
    }

    public int getCalAnalyzeFrequency() {
        return switch (calAnalyze) {
            case "Beginning Only" -> 1;
            case "Beginning-End" -> 2;
            default -> 3;
        };
    }

    public int getLibAnalyzeFrequency() {
        return switch (libAnalyze) {
            case "Beginning Only" -> 1;
            case "Beginning-End" -> 2;
            default -> 3;
        };
    }

    public String getInstrument() {
        return instrument;
    }

    public boolean getNegRun() {
        return negRun;
    }

    public int getQcAnalyzeFrequency() {
        return switch (qcAnalyze) {
            case "Beginning-End" -> 2;
            case "Middle-End" -> 2;
            case "Beginning-Middle-End" -> 3;
            default -> 0;
        };
    }

    public Integer getQcSampleNum() {
        return qcNum;
    }

    public int getSstAnalyzeFrequency() {
        return switch (sstAnalyze) {
            case "Beginning-End" -> 2;
            case "Middle-End" -> 2;
            case "Beginning-Middle-End" -> 3;
            default -> 0;
        };
    }

    public Integer getCalStartPosition() {
        return calStartPosition;
    }

    public int getCalTotalNumber() {
        return calBtmLevel - calTopLevel + 1;
    }

    public int getLibTotalNumber() {
        return (libSchema.contains("DIA")) ? 8 : 6;
    }

    public int getLibPositionNumber() {
        return (libSchema.contains("DIA")) ? 2 : 1;
    }

    public String getCalAnalyze() {
        return calAnalyze;
    }

    public Integer getCalBtmLevel() {
        return calBtmLevel;
    }

    public Boolean getCalRun() {
        return calRun;
    }

    public Integer getCalTopLevel() {
        return calTopLevel;
    }

    public Integer getNegNumACal() {
        return negNumACal;
    }

    public Integer getNegNumAQc() {
        return negNumAQc;
    }

    public Integer getNegNumBCal() {
        return negNumBCal;
    }

    public Integer getNegNumBQc() {
        return negNumBQc;
    }

    public Integer getNegStartPosition() {
        return negStartPosition;
    }

    public String getPrefix() {
        return prefix;
    }

    public Integer getQcStartPosition() {
        return qcStartPosition;
    }

    public String getSampleRandomize() {
        return sampleRandomize;
    }

    public Integer getSampleStartPosition() {
        return sampleStartPosition;
    }

    public Integer getSstSampleNum() {
        return sstNum;
    }

    public Integer getSstStartPosition() {
        return sstStartPosition;
    }

    public int getTotalSampleNumber(int sampleSize) {

        int totalInjections = batchNum;

        if (batchInjections) {

            if (calRun) {
                totalInjections = totalInjections - (getCalTotalNumber() + blankNumBCal + blankNumACal + negNumBCal + negNumACal) * getCalAnalyzeFrequency();
            }

            if (libRun) {
                totalInjections = totalInjections - (getLibTotalNumber() + blankNumBCal + blankNumACal + negNumBCal + negNumACal) * getLibAnalyzeFrequency();
            }

            if (qcRun) {
                if (getQcAnalyzeFrequency() == 0) {
                    totalInjections = totalInjections - (qcLevels + blankNumBQc + blankNumAQc + negNumBQc + negNumAQc);
                } else {
                    totalInjections = totalInjections - (qcLevels + blankNumBQc + blankNumAQc + negNumBQc + negNumAQc) * getQcAnalyzeFrequency();
                }
            }

            if (sstRun) {
                if (getSstAnalyzeFrequency() == 0) {
                    totalInjections = totalInjections - sstLevels;
                } else {
                    totalInjections = totalInjections - sstLevels * getSstAnalyzeFrequency();
                }
            }
        }

        if (batchMax) {
            return totalInjections;
        } else {

            int trueInjections = sampleSize;

            if (getQcAnalyzeFrequency() == 0 && getQcRun() && batchInjections) {
                trueInjections = trueInjections + trueInjections / qcNum + 1;
            }

            if (getSstAnalyzeFrequency() == 0 && getSstRun() && batchInjections) {
                trueInjections = trueInjections + trueInjections / sstNum + 1;
            }

            if (trueInjections > totalInjections) {
                int i = 2;
                while (true) {
                    int testInjection = trueInjections / i + 1;
                    if (testInjection < totalInjections) {
                        return testInjection;
                    }
                    i = i + 1;
                }
            } else {
                return totalInjections;
            }
        }
    }

    public Boolean isTargetedExperiment() {
        return targeted;
    }

    public Boolean getBlankRun() {
        return blankRun;
    }

    public boolean runLibAtTime(String time) {
        return libAnalyze.contains(time);
    }

    public boolean runCalAtTime(String time) {
        return calAnalyze.contains(time);
    }

    public boolean runSstAtTime(String time) {
        return sstAnalyze.contains(time);
    }

    public boolean runQcAtTime(String time) {
        return qcAnalyze.contains(time);
    }

    public boolean getSstRun() {
        return sstRun;
    }

    public boolean getQcRun() {
        return qcRun;
    }

    public int getSstLevels() {
        return this.sstLevels;
    }

    public int getQcLevels() {
        return this.qcLevels;
    }

    public int getQcTotalSample() {
        return qcLevels + blankNumBQc + blankNumAQc + negNumBQc + negNumAQc;
    }

    public Boolean isCountInjection() {
        return batchInjections;
    }

    public Boolean isDiaLibraryExperiment() {
        return this.libSchema.contains("DIA");
    }

    public Boolean getLibRun() {
        return libRun;
    }
}
