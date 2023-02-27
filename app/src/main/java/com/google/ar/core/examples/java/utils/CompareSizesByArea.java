package com.google.ar.core.examples.java.utils;

import android.util.Size;

import java.util.Comparator;

public class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        return (int) Math.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
