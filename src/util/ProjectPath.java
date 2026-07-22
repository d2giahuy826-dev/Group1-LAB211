package util;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves project-relative paths regardless of the terminal working directory. */
public final class ProjectPath {
    private static final String PROJECT_FOLDER = "Group1-LAB211";

    private ProjectPath() { }

    public static Path resolve(String value) {
        Path requested = Path.of(value);
        if (requested.isAbsolute()) return requested.normalize();

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve(requested).normalize();
        Path nested = workingDirectory.resolve(PROJECT_FOLDER).resolve(requested).normalize();
        if (workingDirectory.getFileName() != null
                && PROJECT_FOLDER.equalsIgnoreCase(workingDirectory.getFileName().toString())
                && (Files.exists(direct) || hasExistingParent(direct))) return direct;
        if (Files.exists(nested) || hasExistingParent(nested)) return nested;
        if (Files.exists(direct) || hasExistingParent(direct)) return direct;

        Path codeLocation = codeLocation();
        for (Path current = codeLocation; current != null; current = current.getParent()) {
            Path candidate = current.resolve(requested).normalize();
            if (Files.exists(candidate) || hasExistingParent(candidate)) return candidate;
            Path childCandidate = current.resolve(PROJECT_FOLDER).resolve(requested).normalize();
            if (Files.exists(childCandidate) || hasExistingParent(childCandidate)) return childCandidate;
        }
        return direct;
    }

    private static boolean hasExistingParent(Path path) {
        Path parent = path.getParent();
        return parent != null && Files.isDirectory(parent);
    }

    private static Path codeLocation() {
        try {
            return Path.of(ProjectPath.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException | NullPointerException e) {
            return Path.of("").toAbsolutePath().normalize();
        }
    }
}
