package com.example.emojidumpapp.util;

import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class PrivateTypeface {
    private static final Class FONT_FAMILY_CLASS;
    private static final Constructor FONT_FAMILY_CTOR;
    private static final Method ADD_FONT_METHOD;
    private static final Method FREEZE_METHOD;
    private static final Method CREATE_FROM_FAMILIES;

    static {
        Class fontFamilyClass;
        Constructor fontFamilyCtor;
        Method addFontMethod;
        Method freezeMethod;
        Method createFromFamilies;

        try {
            fontFamilyClass = Class.forName("android.graphics.FontFamily");
            fontFamilyCtor = fontFamilyClass.getConstructor();
            addFontMethod = fontFamilyClass.getMethod("addFontFromBuffer", ByteBuffer.class, Integer.TYPE,
                    FontVariationAxis[].class, Integer.TYPE, Integer.TYPE);
            freezeMethod = fontFamilyClass.getMethod("freeze");
            createFromFamilies = Typeface.class.getDeclaredMethod("createFromFamilies",
                    Class.forName("[L" + fontFamilyClass.getName() + ";"));
            createFromFamilies.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fontFamilyClass = null;
            fontFamilyCtor = null;
            addFontMethod = null;
            freezeMethod = null;
            createFromFamilies = null;
        }
        FONT_FAMILY_CLASS = fontFamilyClass;
        FONT_FAMILY_CTOR = fontFamilyCtor;
        ADD_FONT_METHOD = addFontMethod;
        FREEZE_METHOD = freezeMethod;
        CREATE_FROM_FAMILIES = createFromFamilies;
    }

    private static ByteBuffer mmap(String fullPath) {
        try (FileInputStream file = new FileInputStream(fullPath)) {
            final FileChannel fileChannel = file.getChannel();
            final long fontSize = fileChannel.size();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fontSize);
        } catch (IOException e) {
            Log.e("Error", "Error mapping font file " + fullPath);
            return null;
        }
    }

    public static Typeface buildNoFallbackTypeface(String path) {
        try {
            Object family = FONT_FAMILY_CTOR.newInstance();
            ADD_FONT_METHOD.invoke(family, mmap(path), 0, null, 400, 0);
            FREEZE_METHOD.invoke(family);

            Object ar = Array.newInstance(FONT_FAMILY_CLASS, 1);
            Array.set(ar, 0, family);
            return (Typeface) CREATE_FROM_FAMILIES.invoke(null, ar);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            Log.e("Error", "building", e);
        }
        return Typeface.createFromFile(path);
    }
}
