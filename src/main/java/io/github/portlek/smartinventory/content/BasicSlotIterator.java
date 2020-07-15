/*
 * MIT License
 *
 * Copyright (c) 2020 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.portlek.smartinventory.content;

import com.google.common.base.Preconditions;
import io.github.portlek.smartinventory.Icon;
import io.github.portlek.smartinventory.InventoryContents;
import io.github.portlek.smartinventory.SlotIterator;
import io.github.portlek.smartinventory.util.Pattern;
import io.github.portlek.smartinventory.util.SlotPos;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BasicSlotIterator implements SlotIterator {

    @NotNull
    private final SlotIterator.Type type;

    @NotNull
    private final InventoryContents contents;

    private final int startRow;

    private final int startColumn;

    private final Set<SlotPos> blacklisted = new HashSet<>();

    private boolean started;

    private boolean allowOverride = true;

    private int endRow;

    private int endColumn;

    private int row;

    private int column;

    private int patternRowOffset;

    private int patternColumnOffset;

    @Nullable
    private Pattern<Boolean> pattern;

    private int blacklistPatternRowOffset;

    private int blacklistPatternColumnOffset;

    @Nullable
    private Pattern<Boolean> blacklistPattern;

    public BasicSlotIterator(@NotNull final InventoryContents contents, @NotNull final SlotIterator.Type type) {
        this(contents, type, 0, 0);
    }

    public BasicSlotIterator(@NotNull final InventoryContents contents, @NotNull final SlotIterator.Type type,
                             final int startRow, final int startColumn) {
        this.contents = contents;
        this.type = type;
        this.endRow = this.contents.page().row() - 1;
        this.endColumn = this.contents.page().column() - 1;
        this.startRow = startRow;
        this.row = startRow;
        this.startColumn = startColumn;
        this.column = startColumn;
    }

    @NotNull
    @Override
    public Optional<Icon> get() {
        return this.contents.get(this.row, this.column);
    }

    @NotNull
    @Override
    public SlotIterator set(@NotNull final Icon item) {
        if (this.canPlace()) {
            this.contents.set(this.row, this.column, item);
        }

        return this;
    }

    @NotNull
    @Override
    public SlotIterator previous() {
        if (this.row == 0 && this.column == 0) {
            this.started = true;
            return this;
        }
        do {
            if (this.started) {
                if (this.type == SlotIterator.Type.HORIZONTAL) {
                    this.column--;
                    if (this.column == 0) {
                        this.column = this.contents.page().column() - 1;
                        this.row--;
                    }
                } else if (this.type == SlotIterator.Type.VERTICAL) {
                    this.row--;
                    if (this.row == 0) {
                        this.row = this.contents.page().row() - 1;
                        this.column--;
                    }
                }
            } else {
                this.started = true;
            }
        }
        while (!this.canPlace() && (this.row != 0 || this.column != 0));

        return this;
    }

    @NotNull
    @Override
    public SlotIterator next() {
        if (this.ended()) {
            this.started = true;
            return this;
        }

        do {
            if (this.started) {
                switch (this.type) {
                    case HORIZONTAL:
                        ++this.column;
                        this.column %= this.contents.page().column();

                        if (this.column == 0) {
                            this.row++;
                        }
                        break;
                    case VERTICAL:
                        ++this.row;
                        this.row %= this.contents.page().row();

                        if (this.row == 0) {
                            this.column++;
                        }
                        break;
                }
            } else {
                this.started = true;
            }
        }
        while (!this.canPlace() && !this.ended());

        return this;
    }

    @NotNull
    @Override
    public SlotIterator blacklist(final int index) {
        final int count = this.contents.page().column();
        this.blacklisted.add(SlotPos.of(index / count, index % count));
        return this;
    }

    @NotNull
    @Override
    public SlotIterator blacklist(final int row, final int column) {
        this.blacklisted.add(SlotPos.of(row, column));
        return this;
    }

    @NotNull
    @Override
    public SlotIterator blacklist(@NotNull final SlotPos slotPos) {
        return this.blacklist(slotPos.getRow(), slotPos.getColumn());
    }

    @Override
    public int row() {
        return this.row;
    }

    @NotNull
    @Override
    public SlotIterator row(final int row) {
        this.row = row;
        return this;
    }

    @Override
    public int column() {
        return this.column;
    }

    @NotNull
    @Override
    public SlotIterator column(final int column) {
        this.column = column;
        return this;
    }

    @NotNull
    @Override
    public SlotIterator reset() {
        this.started = false;
        this.row = this.startRow;
        this.column = this.startColumn;
        return this;
    }

    @Override
    public boolean started() {
        return this.started;
    }

    @Override
    public boolean ended() {
        return this.row == this.endRow
            && this.column == this.endColumn;
    }

    @NotNull
    @Override
    public SlotIterator endPosition(int row, int column) {
        if (row < 0) {
            row = this.contents.page().row() - 1;
        }
        if (column < 0) {
            column = this.contents.page().column() - 1;
        }
        Preconditions.checkArgument(row * column >= this.startRow * this.startColumn,
            "The end position needs to be after the start of the slot iterator");
        this.endRow = row;
        this.endColumn = column;
        return this;
    }

    @NotNull
    @Override
    public SlotIterator endPosition(@NotNull final SlotPos endPosition) {
        return this.endPosition(endPosition.getRow(), endPosition.getColumn());
    }

    @Override
    public boolean doesAllowOverride() {
        return this.allowOverride;
    }

    @NotNull
    @Override
    public SlotIterator allowOverride(final boolean override) {
        this.allowOverride = override;
        return this;
    }

    @NotNull
    @Override
    public SlotIterator withPattern(@NotNull final Pattern<Boolean> pattern) {
        return this.withPattern(pattern, 0, 0);
    }

    @NotNull
    @Override
    public SlotIterator withPattern(@NotNull final Pattern<Boolean> pattern, final int rowOffset,
                                    final int columnOffset) {
        this.patternRowOffset = rowOffset;
        this.patternColumnOffset = columnOffset;
        if (!pattern.getDefaultValue().isPresent()) {
            pattern.setDefault(false);
        }
        this.pattern = pattern;
        return this;
    }

    @NotNull
    @Override
    public SlotIterator blacklistPattern(@NotNull final Pattern<Boolean> pattern) {
        return this.blacklistPattern(pattern, 0, 0);
    }

    @NotNull
    @Override
    public SlotIterator blacklistPattern(@NotNull final Pattern<Boolean> pattern, final int rowOffset,
                                         final int columnOffset) {
        this.blacklistPatternRowOffset = rowOffset;
        this.blacklistPatternColumnOffset = columnOffset;
        if (!pattern.getDefaultValue().isPresent()) {
            pattern.setDefault(false);
        }
        this.blacklistPattern = pattern;
        return this;
    }

    private boolean canPlace() {
        final AtomicBoolean patternAllows = new AtomicBoolean(true);
        Optional.ofNullable(this.pattern).ifPresent(booleanPattern ->
            patternAllows.set(this.checkPattern(booleanPattern, this.patternRowOffset, this.patternColumnOffset)));
        final AtomicBoolean blacklistPatternAllows = new AtomicBoolean(true);
        Optional.ofNullable(this.blacklistPattern).ifPresent(booleanPattern ->
            blacklistPatternAllows.set(!this.checkPattern(booleanPattern, this.blacklistPatternRowOffset, this.blacklistPatternColumnOffset)));
        return !this.blacklisted.contains(SlotPos.of(this.row, this.column)) &&
            (this.allowOverride || !this.get().isPresent()) &&
            patternAllows.get() &&
            blacklistPatternAllows.get();
    }

    private boolean checkPattern(@NotNull final Pattern<Boolean> pattern, final int rowOffset,
                                 final int columnOffset) {
        final Optional<Boolean> object = pattern.getObject(this.row - rowOffset, this.column - columnOffset);
        if (pattern.isWrapAround()) {
            return object.orElse(false);
        }
        return this.row >= rowOffset && this.column >= columnOffset &&
            this.row < pattern.getRowCount() + rowOffset &&
            this.column < pattern.getColumnCount() + columnOffset &&
            object.orElse(false);
    }

}