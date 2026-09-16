package com.igot.cb.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FileProcessServiceTest {

    @InjectMocks
    private FileProcessService fileProcessService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProcessExcelFile_Success() throws IOException {
        MultipartFile file = createTestExcelFile();
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull("Result should not be null", result);
        assertEquals("Should have 2 data rows", 2, result.size());
        assertEquals("First row, first column should match", "Value1", result.get(0).get("Header1"));
        assertEquals("First row, second column should match", "Value2", result.get(0).get("Header2"));
        assertEquals("Second row, first column should match", "Value3", result.get(1).get("Header1"));
        assertEquals("Second row, second column should match", "Value4", result.get(1).get("Header2"));
    }

    @Test
    public void testProcessCsvFile_Success() {
        MultipartFile file = createTestCsvFile();
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull("Result should not be null", result);
        assertEquals("Should have 2 data rows", 2, result.size());
        assertEquals("First row, first column should match", "Value1", result.get(0).get("Header1"));
        assertEquals("First row, second column should match", "Value2", result.get(0).get("Header2"));
        assertEquals("Second row, first column should match", "Value3", result.get(1).get("Header1"));
        assertEquals("Second row, second column should match", "Value4", result.get(1).get("Header2"));
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_NullFileName() {
        MockMultipartFile file = new MockMultipartFile("file", null, "application/vnd.ms-excel", new byte[0]);
        fileProcessService.processExcelFile(file);
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_UnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        fileProcessService.processExcelFile(file);
    }

    @Test
    public void testProcessExcelFile_EmptyRows() throws IOException {
        MultipartFile file = createExcelFileWithEmptyRows();
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull("Result should not be null", result);
        assertEquals("Should have 1 data row (ignoring empty rows)", 1, result.size());
        assertEquals("First row, first column should match", "Value1", result.get(0).get("Header1"));
    }

    @Test
    public void testProcessExcelFile_WithDates() throws IOException {
        MultipartFile file = createExcelFileWithDates();
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull("Result should not be null", result);
        assertEquals("Should have 1 data row", 1, result.size());
        assertTrue("Date should be formatted correctly", 
                result.get(0).get("Date").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    private MultipartFile createTestExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Header1");
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Header2");
        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Value1");
        dataRow1.createCell(1).setCellValue("Value2");
        Row dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("Value3");
        dataRow2.createCell(1).setCellValue("Value4");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        workbook.write(byteArrayOutputStream);
        workbook.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                byteArrayOutputStream.toByteArray());
    }
    
    private MultipartFile createTestCsvFile() {
        String csvContent = "Header1,Header2\nValue1,Value2\nValue3,Value4";
        return new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
    }
    
    private MultipartFile createExcelFileWithEmptyRows() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Header1");
        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Value1");
        sheet.createRow(2);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        workbook.write(byteArrayOutputStream);
        workbook.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                byteArrayOutputStream.toByteArray());
    }
    
    private MultipartFile createExcelFileWithDates() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Date");
        Row dataRow = sheet.createRow(1);
        Cell dateCell = dataRow.createCell(0);
        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        dateCell.setCellStyle(cellStyle);
        dateCell.setCellValue(new java.util.Date());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        workbook.write(byteArrayOutputStream);
        workbook.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                byteArrayOutputStream.toByteArray());
    }

    @Test
    public void testProcessCsvFile_EmptyRowStopsProcessing() {
        String csvContent = "Header1,Header2\nValue1,Value2\n,";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals("Only first row should be processed", 1, result.size());
    }

    @Test
    public void testProcessCsvFile_WithDates() {
        String dateValue = "2024-01-01T10:15:30.000Z";
        String csvContent = "Header1\n" + dateValue + "\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertTrue("Date should be formatted correctly",
                result.get(0).get("Header1").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    @Test(expected = RuntimeException.class)
    public void testProcessCsvFile_MalformedContentThrowsException() {
        String badCsvContent = "\"Header1,Header2\n\"UnclosedQuote,Value2";
        MultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", badCsvContent.getBytes());
        fileProcessService.processExcelFile(file);
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_CorruptedWorkbookThrowsException() {
        byte[] badData = "NotAnExcelFile".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", badData);
        fileProcessService.processExcelFile(file);
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_UnsupportedFileTypeUpperCase() {
        MockMultipartFile file = new MockMultipartFile("file", "test.TXT", "text/plain", "test content".getBytes());
        fileProcessService.processExcelFile(file);
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_IOExceptionFromInputStream() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.xlsx");
        when(file.getInputStream()).thenThrow(new IOException("Stream error"));
        fileProcessService.processExcelFile(file);
    }

    @Test
    public void testProcessCsvFile_WithNonDateValue() {
        String csvContent = "Header1\nNotADate\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals("NotADate", result.get(0).get("Header1"));
    }

    @Test
    public void testProcessCsvFile_BadDateFormatHandledGracefully() {
        String csvContent = "Header1\n2024/01/01\n"; // invalid format for parseDate
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2024/01/01", result.get(0).get("Header1")); // value is preserved
    }

    @Test
    public void testProcessExcelFile_HeaderCellIsNull() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        sheet.createRow(0).createCell(0, CellType.BLANK);
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Value1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertTrue("No rows should be processed when header is blank", result.isEmpty());
    }

    @Test
    public void testProcessExcelFile_NoHeaderRow_ReturnsEmptyList() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        workbook.createSheet("Test Sheet");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull(result);
        assertTrue("Should return empty list when no header row exists", result.isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void testProcessExcelFile_EmptyFileThrowsException() {
        MultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        fileProcessService.processExcelFile(file);
    }

    @Test(expected = RuntimeException.class)
    public void testProcessCsvFile_MismatchedColumns() {
        String csvContent = "Header1,Header2\nValue1\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        fileProcessService.processExcelFile(file);
    }

    @Test
    public void testProcessExcelFile_WrongMimeTypeButCorrectExtension() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "text/plain", "not really excel".getBytes());
        try {
            fileProcessService.processExcelFile(file);
            fail("Expected RuntimeException for wrong MIME type");
        } catch (RuntimeException e) {
            assertTrue(
                    e.getMessage().contains("Error processing file")
                            || e.getMessage().contains("unsupported file type")
            );
        }
    }

    @Test
    public void testProcessCsvFile_ExtraColumnsIgnored() {
        String csvContent = "Header1,Header2\nValue1,Value2,ExtraValue\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertEquals("Value1", result.get(0).get("Header1"));
        assertEquals("Value2", result.get(0).get("Header2"));
        assertNull(result.get(0).get("ExtraColumn"));
    }

    @Test
    public void testProcessExcelFile_WithNumericValues() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Number");
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(12345.67);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertTrue(result.get(0).get("Number").contains("12345"));
    }

    @Test(expected = RuntimeException.class)
    public void testProcessFile_UnsupportedExtensionButValidMime() {
        MultipartFile file = new MockMultipartFile("file", "test.unknown",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "fake".getBytes());
        fileProcessService.processExcelFile(file);
    }

    @Test
    public void testProcessCsvFile_OnlyHeaders() {
        String csvContent = "Header1,Header2\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertNotNull(result);
        assertTrue("Should return empty list when only headers are present", result.isEmpty());
    }

    @Test
    public void testProcessExcelFile_MultipleSheets() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet1 = workbook.createSheet("Sheet1");
        Row headerRow = sheet1.createRow(0);
        headerRow.createCell(0).setCellValue("Header1");
        Row dataRow = sheet1.createRow(1);
        dataRow.createCell(0).setCellValue("Value1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertEquals("Value1", result.get(0).get("Header1"));
    }

    @Test
    public void testProcessExcelFile_RowWithPartialEmptyCells() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Col1");
        headerRow.createCell(1).setCellValue("Col2");
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Value1");
        dataRow.createCell(1).setBlank(); // simulate empty cell
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertEquals("Value1", result.get(0).get("Col1"));
        assertTrue(result.get(0).get("Col2") == null || result.get(0).get("Col2").isEmpty());
    }


    @Test
    public void testProcessCsvFile_QuotedValuesWithCommas() {
        String csvContent = "Header1,Header2\n\"Value,With,Comma\",Value2\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertEquals("Value,With,Comma", result.get(0).get("Header1"));
        assertEquals("Value2", result.get(0).get("Header2"));
    }

    @Test
    public void testProcessExcelFile_WithFormulaCell() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Calc");
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellFormula("1+2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        assertTrue(result.get(0).get("Calc").contains("3") || result.get(0).get("Calc").contains("1+2"));
    }

    @Test
    public void testProcessCsvFile_TrailingEmptyLine() {
        String csvContent = "Header1,Header2\nValue1,Value2\n\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);

        assertEquals(1, result.size()); // last blank row skipped
        assertEquals("Value1", result.get(0).get("Header1"));
    }

    @Test
    public void testProcessCsvFile_LargeInput() {
        StringBuilder csv = new StringBuilder("Header1\n");
        for (int i = 0; i < 1000; i++) {
            csv.append("Value").append(i).append("\n");
        }
        MultipartFile file = new MockMultipartFile("file", "big.csv", "text/csv", csv.toString().getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1000, result.size());
        assertEquals("Value0", result.get(0).get("Header1"));
        assertEquals("Value999", result.get(999).get("Header1"));
    }

    @Test
    public void testProcessCsvFile_DuplicateHeaders_OverwritesValue() {
        String csvContent = "Header1,Header1\nValue1,Value2\n";
        MultipartFile file = new MockMultipartFile("file", "dup.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        Map<String, String> row = result.get(0);
        assertTrue("Header1 should exist", row.containsKey("Header1"));
        assertEquals("Duplicate header should overwrite previous value", "Value2", row.get("Header1"));
    }


    @Test
    public void testProcessExcelFile_MixedCellTypes() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Col1");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(true); // boolean cell

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals("true", result.get(0).get("Col1").toLowerCase());
    }


    @Test
    public void testProcessExcelFile_MalformedDateHandledGracefully() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Date");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("not-a-date"); // force string into date column

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals("not-a-date", result.get(0).get("Date"));
    }

    @Test
    public void testProcessCsvFile_SemicolonDelimiter_TreatedAsSingleColumn() {
        String csvContent = "Header1;Header2\nValue1;Value2\n";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());
        List<Map<String, String>> result = fileProcessService.processExcelFile(file);
        assertEquals(1, result.size());
        Map<String, String> row = result.get(0);
        assertTrue(row.containsKey("Header1;Header2"));
        assertEquals("Value1;Value2", row.get("Header1;Header2"));
    }
}