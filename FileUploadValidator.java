import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.clamav4j.ClamAVClient;
import org.clamav4j.ClamAVException;
import org.clamav4j.ScanResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUploadValidator {
    public static boolean isPasswordProtected(String filePath, FileType fileType) {
        switch (fileType) {
            case PDF:
                return isPasswordProtectedPDF(filePath);
            case WORD:
                return isPasswordProtectedWord(filePath);
            case EXCEL:
                return isPasswordProtectedExcel(filePath);
            case POWERPOINT:
                return isPasswordProtectedPowerPoint(filePath);
            case TEXT:
                return false; // No password protection for text files
            default:
                return false;
        }
    }

    private static boolean isPasswordProtectedPDF(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.isEncrypted();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isPasswordProtectedWord(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            XWPFDocument document = new XWPFDocument(fileInputStream);
            return document.getPackagePart().getPackageProperties().getEncryptionData() != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isPasswordProtectedExcel(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            Workbook workbook;
            if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fileInputStream);
            } else if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } else {
                return false; // Invalid file extension
            }
            return workbook.isWriteProtected();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isPasswordProtectedPowerPoint(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            XMLSlideShow slideShow = new XMLSlideShow(fileInputStream);
            return slideShow.getPackagePart().getPackageProperties().getEncryptionData() != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasMaliciousContent(String filePath) {
        try (ClamAVClient client = new ClamAVClient("localhost", 3310)) {
            byte[] fileBytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(filePath));
            ScanResult result = client.scan(fileBytes);
            return result.isInfected();
        } catch (ClamAVException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        String filePath = "path/to/uploaded/file.xlsx"; // Replace with the actual filepath
        FileType fileType = getFileType(filePath);

        if (isPasswordProtected(filePath, fileType)) {
            // Quarantine the file and display an error message
            quarantineFile(filePath);
            System.out.println("Password-protected files are not allowed.");
        } else if (hasMaliciousContent(filePath)) {
            // Quarantine the file and display an error message
            quarantineFile(filePath);
            System.out.println("The file contains malicious content.");
        } else {
            // Continue processing the uploaded file
            processUploadedFile(filePath, fileType);
        }
    }

    private static FileType getFileType(String filePath) {
        if (filePath.endsWith(".pdf")) {
            return FileType.PDF;
        } else if (filePath.endsWith(".docx")) {
            return FileType.WORD;
        } else if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
            return FileType.EXCEL;
        } else if (filePath.endsWith(".pptx")) {
            return FileType.POWERPOINT;
        } else if (filePath.endsWith(".txt")) {
            return FileType.TEXT;
        } else {
            return FileType.UNKNOWN;
        }
    }

    private static void processUploadedFile(String filePath, FileType fileType) {
        // Perform actions on the uploaded file based on its type
        switch (fileType) {
            case PDF:
                processPDF(filePath);
                break;
            case WORD:
                processWord(filePath);
                break;
            case EXCEL:
                processExcel(filePath);
                break;
            case POWERPOINT:
                processPowerPoint(filePath);
                break;
            case TEXT:
                processText(filePath);
                break;
            default:
                System.out.println("Unsupported file type.");
        }
    }

    private static void processPDF(String filePath) {
        // Process the uploaded PDF file
        System.out.println("Processing the uploaded PDF: " + filePath);
    }

    private static void processWord(String filePath) {
        // Process the uploaded Word file
        System.out.println("Processing the uploaded Word document: " + filePath);
    }

    private static void processExcel(String filePath) {
        // Process the uploaded Excel file
        System.out.println("Processing the uploaded Excel spreadsheet: " + filePath);
    }

    private static void processPowerPoint(String filePath) {
        // Process the uploaded PowerPoint file
        System.out.println("Processing the uploaded PowerPoint presentation: " + filePath);
    }

    private static void processText(String filePath) {
        // Process the uploaded text file
        System.out.println("Processing the uploaded text file: " + filePath);
    }

    private static void quarantineFile(String filePath) {
        try {
            // Replace "quarantine" with the desired quarantine directory path
            Path source = Path.of(filePath);
            Path destination = Path.of("quarantine/" + source.getFileName());
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum FileType {
        PDF,
        WORD,
        EXCEL,
        POWERPOINT,
        TEXT,
        UNKNOWN
    }
}
