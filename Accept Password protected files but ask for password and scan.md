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
