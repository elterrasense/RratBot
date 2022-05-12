package com.rrat.ogey.model;

import org.springframework.lang.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkovModels {
    private MarkovModels() {}

    public static MarkovModel load(Path file) {
        if (Files.exists(file)) {
            try (FileInputStream is = new FileInputStream(file.toFile())) {
                return load(is);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            return null;
        }
    }

    public static void save(Path file, MarkovModel model) {
        try (FileOutputStream os = new FileOutputStream(file.toFile())) {
            save(os, model);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static MarkovModel loadResource(String name) {
        try (InputStream is = MarkovModels.class.getResourceAsStream(name)) {
            return load(is);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static MarkovModel load(InputStream is) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            return (MarkovModel) ois.readObject();
        } catch (ClassNotFoundException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void save(OutputStream os, MarkovModel model) {
        try {
            ObjectOutputStream ois = new ObjectOutputStream(os);
            ois.writeObject(model);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
