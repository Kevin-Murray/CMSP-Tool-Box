package cmsp.tool.box.enums;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 * Enumerator class for export of discovery project report into Excel document
 */
public enum DiscoveryReportColors {

    header("DAE7F5", null, HorizontalAlignment.CENTER, false),
    headerB("DAE7F5", null, HorizontalAlignment.CENTER, true),
    defaultEvenCenter("FFFFFF", null, HorizontalAlignment.CENTER, false),
    defaultOddCenter("F6F6F6", null, HorizontalAlignment.CENTER, false),
    defaultEvenLeft("FFFFFF", null, HorizontalAlignment.LEFT, false),
    defaultOddLeft("F6F6F6", null, HorizontalAlignment.LEFT, false),
    defaultEvenCenterB("FFFFFF", null, HorizontalAlignment.CENTER, true),
    defaultOddCenterB("F6F6F6", null, HorizontalAlignment.CENTER, true),
    defaultEvenLeftB("FFFFFF", null, HorizontalAlignment.LEFT, true),
    defaultOddLeftB("F6F6F6", null, HorizontalAlignment.LEFT, true),
    contaminantEvenCenter("FFCDD4", null, HorizontalAlignment.CENTER, false),
    contaminantOddCenter("FFB6C1", null, HorizontalAlignment.CENTER, false),
    contaminantEvenLeft("FFCDD4", null, HorizontalAlignment.LEFT, false),
    contaminantOddLeft("FFB6C1", null, HorizontalAlignment.LEFT, false),
    missingEven("D9D9D9", null, HorizontalAlignment.CENTER, false),
    missingOdd("BFBFBF", null, HorizontalAlignment.CENTER, false),
    valueGoodEven("D9F2D0", null, HorizontalAlignment.CENTER, false),
    valueGoodOdd("B4E5A2", null, HorizontalAlignment.CENTER, false),
    valueMediumEven("FFF5C9", null, HorizontalAlignment.CENTER, false),
    valueMediumOdd("FFEB9C", null, HorizontalAlignment.CENTER, false),
    valueBadEven("FFD1D8", null, HorizontalAlignment.CENTER, false),
    valueBadOdd("FFB6C1", null, HorizontalAlignment.CENTER, false),
    ratioHigh1("FBD5D6", null, HorizontalAlignment.CENTER, false),
    ratioHigh2("F7ABAC", null, HorizontalAlignment.CENTER, false),
    ratioHigh3("F28082", null, HorizontalAlignment.CENTER, false),
    ratioHigh4("EE5658", null, HorizontalAlignment.CENTER, false),
    ratioHigh5("E92B2E", "FFFF00", HorizontalAlignment.CENTER, false),
    ratioLow1("D9DEED", null, HorizontalAlignment.CENTER, false),
    ratioLow2("B3BCDB", null, HorizontalAlignment.CENTER, false),
    ratioLow3("8D9AC9", null, HorizontalAlignment.CENTER, false),
    ratioLow4("6778B7", null, HorizontalAlignment.CENTER, false),
    ratioLow5("4156A5", "FFFF00", HorizontalAlignment.CENTER, false),
    pvalSig1("FFFFCC", null, HorizontalAlignment.CENTER, false),
    pvalSig2("C2E699", null, HorizontalAlignment.CENTER, false),
    pvalSig3("78C679", null, HorizontalAlignment.CENTER, false),
    pvalSig4("31A354", "FFFF00", HorizontalAlignment.CENTER, false),
    coverage0("F1F9FF", null, HorizontalAlignment.CENTER, false),
    coverage1("E3F3FE", null, HorizontalAlignment.CENTER, false),
    coverage2("D5EDFE", null, HorizontalAlignment.CENTER, false),
    coverage3("C7E7FD", null, HorizontalAlignment.CENTER, false),
    coverage4("B9E0FD", null, HorizontalAlignment.CENTER, false),
    coverage5("ABDAFD", null, HorizontalAlignment.CENTER, false),
    coverage6("9DD4FC", null, HorizontalAlignment.CENTER, false),
    coverage7("8FCEFC", null, HorizontalAlignment.CENTER, false),
    coverage8("81C8FB", null, HorizontalAlignment.CENTER, false),
    coverage9("73C2FB", null, HorizontalAlignment.CENTER, false);

    private final HorizontalAlignment alignment;
    private final Boolean boundary;
    private final String fillColorCode;
    private final String fontColorCode;

    /**
     * Report codes for cell style formatting.
     *
     * @param fillColorCode Hexcode of fill color
     * @param fontColorCode Hexcode of font color - null indicates default color
     * @param alignment     Cell horizontal alignment
     * @param boundary      Indicator if double boundary border should be drawn on left side of cell
     */
    DiscoveryReportColors(String fillColorCode, String fontColorCode, HorizontalAlignment alignment, Boolean boundary) {
        this.fillColorCode = fillColorCode;
        this.fontColorCode = fontColorCode;
        this.alignment = alignment;
        this.boundary = boundary;
    }

    public HorizontalAlignment getAlignment() {
        return alignment;
    }

    public Boolean getBoundary() {
        return boundary;
    }

    public XSSFColor getFillColor() {
        try {
            return new XSSFColor(Hex.decodeHex(fillColorCode), null);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFillColorCode() {
        return (fillColorCode != null) ? "#" + fillColorCode : null;
    }

    public XSSFColor getFontColor() {
        try {
            return new XSSFColor(Hex.decodeHex(fontColorCode), null);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFontColorCode() {
        return (fontColorCode != null) ? "#" + fontColorCode : null;
    }
}
