To modify the code to ask for the password when a password-protected file is detected instead of rejecting it, you can make the following changes:

1. Update the `isPasswordProtected` method in the `FileUploadValidator` class to return `true` if the file is password-protected and `false` otherwise. Remove the exception `throws IOException` from the method signature.

```java
private static boolean isPasswordProtected(String filePath, FileType fileType) {
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
```

2. Modify the `main` method to prompt for a password when a password-protected file is detected and ask the user to provide the password. You can use the `Scanner` class to read user input. Replace the existing code inside the `if (isPasswordProtected(filePath, fileType))` block with the following:

```java
if (isPasswordProtected(filePath, fileType)) {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter the password for the file: ");
    String password = scanner.nextLine();

    if (isPasswordCorrect(filePath, fileType, password)) {
        // Continue processing the uploaded file
        processUploadedFile(filePath, fileType);
    } else {
        // Reject the file and move it to the quarantine folder
        quarantineFile(filePath);
        System.out.println("Incorrect password. The file has been moved to the quarantine folder.");
    }
} else if (hasMaliciousContent(filePath)) {
    // Reject the file and move it to the quarantine folder
    quarantineFile(filePath);
    System.out.println("The file contains malicious content.");
} else {
    // Continue processing the uploaded file
    processUploadedFile(filePath, fileType);
}
```

3. Add a new method `isPasswordCorrect` to validate the password for different file types. Here's an example implementation for PDF files:

```java
private static boolean isPasswordCorrect(String filePath, FileType fileType, String password) {
    switch (fileType) {
        case PDF:
            return isPasswordCorrectPDF(filePath, password);
        // Add similar methods for other file types if needed
        default:
            return false;
    }
}

private static boolean isPasswordCorrectPDF(String filePath, String password) {
    try (PDDocument document = PDDocument.load(new File(filePath), password)) {
        // If the document loads successfully, the password is correct
        return true;
    } catch (IOException e) {
        // Incorrect password or other error occurred
        return false;
    }
}
```

Make sure to update the code accordingly for other file types if needed.

With these modifications, when a password-protected file is detected, the program will prompt the user to enter the password. If the password is correct, the file will continue to be processed. Otherwise, the file will be moved to the quarantine folder.

Full code will look something like this


java
Copy code
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.clamav4j.ClamAVClient;
import org.clamav4j.ScanResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class FileUploadValidator {
    private static final String QUARANTINE_FOLDER = "/path/to/quarantine/folder/";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s.-]+$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".xlsx", ".xls", ".pptx", ".txt");

    public static boolean hasMaliciousContent(String filePath, String password) {
        try (ClamAVClient client = new ClamAVClient("localhost", 3310)) {
            byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
            ScanResult result = client.scan(fileBytes, password);
            return result.isInfected();
        } catch (IOException e) {
            // Handle exceptions securely (log, display an error message, etc.)
            return false;
        }
    }

    public static void quarantineFile(String filePath) {
        try {
            Path sourceFile = Path.of(filePath);
            Path targetFile = Path.of(QUARANTINE_FOLDER, sourceFile.getFileName().toString());
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Handle exceptions securely (log, display an error message, etc.)
        }
    }

    public static void processUploadedFile(String filePath) {
        // Implement the logic to process the uploaded file here
        System.out.println("Processing file: " + filePath);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the file path: ");
        String filePath = scanner.nextLine();

        FileType fileType = getFileType(filePath);

        if (fileType == FileType.UNKNOWN) {
            System.out.println("Unsupported file format.");
            return;
        }

        if (!isValidFileName(filePath)) {
            System.out.println("Invalid file name.");
            return;
        }

        System.out.print("Enter the password for the file: ");
        String password = scanner.nextLine();

        if (hasMaliciousContent(filePath, password)) {
            quarantineFile(filePath);
            System.out.println("The file contains malicious content.");
        } else {
            processUploadedFile(filePath);
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

    private static boolean isValidFileName(String filePath) {
        String fileName = new File(filePath).getName();
        return FILENAME_PATTERN.matcher(fileName).matches();
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
Please note that you need to replace /path/to/quarantine/folder/ with the actual path to your quarantine folder.
