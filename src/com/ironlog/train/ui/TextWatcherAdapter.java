package com.ironlog.train.ui;

import android.text.Editable;
import android.text.TextWatcher;

/** Minimal TextWatcher so screens don't need three empty methods each time. */
public abstract class TextWatcherAdapter implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public abstract void afterTextChanged(Editable s);
}
