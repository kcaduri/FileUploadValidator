import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class FileUploadValidator {
    private static final String QUARANTINE_FOLDER = "/path/to/quarantine/folder/"; 
    //Make sure to update the QUARENTINE_FOLDER variable to actual path to your quarentine folder.
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s.-]+$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".xlsx", ".xls", ".pptx", ".txt");

    public static boolean isPasswordProtected(String filePath, FileType fileType) throws IOException {
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

    private static boolean isPasswordProtectedPDF(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.isEncrypted();
        }
    }

     private static boolean containsJavaScriptInPDF(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (document.isEncrypted()) {
                return false; // Cannot inspect encrypted files
            }
            // Check for OpenAction JavaScript
            if (document.getDocumentCatalog().getOpenAction() instanceof PDActionJavaScript) {
                return true;
            }
            // Check for JavaScript in the Names dictionary
            if (document.getDocumentCatalog().getNames() != null 
                && document.getDocumentCatalog().getNames().getJavaScript() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPasswordProtectedWord(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            XWPFDocument document = new XWPFDocument(fileInputStream);
            return document.getPackagePart().getPackageProperties().getEncryptionData() != null;
        }
    }

    private static boolean isPasswordProtectedExcel(String filePath) throws IOException {
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
        }
    }

    private static boolean isPasswordProtectedPowerPoint(String filePath) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
            XMLSlideShow slideShow = new XMLSlideShow(fileInputStream);
            return slideShow.getPackagePart().getPackageProperties().getEncryptionData() != null;
        }
    }

    public static boolean hasMaliciousContent(String filePath) throws IOException {
        try (ClamAVClient client = new ClamAVClient("localhost", 3310)) {
            byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
            ScanResult result = client.scan(fileBytes);
            return result.isInfected();
        }
    }

    public static void main(String[] args) {
        String filePath = "path/to/uploaded/file.xlsx"; //set Filepath varible per your application

        try {
            String fileName = getFileName(filePath);
            if (!isFileNameValid(fileName)) {
                // Reject the file and move it to the quarantine folder
                quarantineFile(filePath);
                System.out.println("Invalid file name. Only alphanumeric characters, hyphens, spaces, and periods are allowed.");
            } else if (!isFileTypeValid(filePath)) {
                // Reject the file and move it to the quarantine folder
                quarantineFile(filePath);
                System.out.println("Invalid file type or extension.");
            } else {
                FileType fileType = getFileType(filePath);
                if (isPasswordProtected(filePath, fileType)) {
                    // Reject the file and move it to the quarantine folder
                    quarantineFile(filePath);
                    System.out.println("Password-protected files are not allowed.");
                } else if (hasMaliciousContent(filePath)) {
                    // Reject the file and move it to the quarantine folder
                    quarantineFile(filePath);
                    System.out.println("The file contains malicious content.");
                } else if (fileType == FileType.PDF && containsJavaScriptInPDF(filePath)) {
                    quarantineFile(filePath);
                    System.out.println("The PDF contains JavaScript, which is not allowed."); 
                } else {
                    // Continue processing the uploaded file
                    processUploadedFile(filePath, fileType);
                }
            }
        } catch (IOException e) {
            // Handle exceptions securely (log, display an error message, etc.)
            System.out.println("An error occurred while processing the file.");
            e.printStackTrace();
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

    private static boolean isFileTypeValid(String filePath) {
        String fileExtension = getFileExtension(filePath);
        return ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    private static String getFileExtension(String filePath) {
        int extensionIndex = filePath.lastIndexOf('.');
        if (extensionIndex >= 0 && extensionIndex < filePath.length() - 1) {
            return filePath.substring(extensionIndex);
        }
        return "";
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
        // Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        System.out.println("Processing the uploaded PDF: " + getFileDisplayName(filePath));
    }

    private static void processWord(String filePath) {
        //Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        System.out.println("Processing the uploaded Word document: " + getFileDisplayName(filePath));
    }

    private static void processExcel(String filePath) {
        // Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        System.out.println("Processing the uploaded Excel spreadsheet: " + getFileDisplayName(filePath));
    }

    private static void processPowerPoint(String filePath) {
        // Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        System.out.println("Processing the uploaded PowerPoint presentation: " + getFileDisplayName(filePath));
    }

    private static void processText(String filePath) {
        // Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        System.out.println("Processing the uploaded text file: " + getFileDisplayName(filePath));
    }

    private static String getFileDisplayName(String filePath) {
        // Process the uploaded PDF file, only display file path in logs if need, from a security standpoint this should be set to a generic message.
        return new File(filePath).getName();
    }

    private static boolean isFileNameValid(String fileName) {
        // Check if the file name is valid (only alphanumeric, hyphen, spaces, and periods)
        return FILENAME_PATTERN.matcher(fileName).matches();
    }

    private static String getFileName(String filePath) {
        // Extracts the file name from the file path
        return new File(filePath).getName();
    }

    private static void quarantineFile(String filePath) throws IOException {
        // Move the file to the quarantine folder
        Path sourceFile = Path.of(filePath);
        Path targetFile = Path.of(QUARANTINE_FOLDER, sourceFile.getFileName().toString());
        Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
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
