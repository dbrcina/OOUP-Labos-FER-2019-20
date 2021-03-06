package hr.fer.zemris.ooup.editor.model;

import hr.fer.zemris.ooup.editor.command.*;
import hr.fer.zemris.ooup.editor.observer.CursorObserver;
import hr.fer.zemris.ooup.editor.observer.SelectionObserver;
import hr.fer.zemris.ooup.editor.observer.SelectionState;
import hr.fer.zemris.ooup.editor.observer.TextObserver;
import hr.fer.zemris.ooup.editor.singleton.UndoManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TextEditorModel {

    private static final String LINE_SEPARATOR = "\\r?\\n";

    private List<String> lines = new CopyOnWriteArrayList<>();

    private final Location cursorLocation = new Location();
    private int cursorSize;
    private final LocationRange selectionRange = new LocationRange();

    private final List<CursorObserver> cursorObservers = new LinkedList<>();
    private final List<TextObserver> textObservers = new LinkedList<>();
    private final List<SelectionObserver> selectionObservers = new LinkedList<>();

    private final UndoManager manager = UndoManager.getInstance();

    /* ################################################ */

    /* Constructor */
    public TextEditorModel(String text) {
        lines.addAll(Arrays.asList(text.split(LINE_SEPARATOR)));
    }
    /* ----------- */

    /* ################################################ */

    /* Methods used for cursor manipulation */
    public Location getCursorLocation() {
        return cursorLocation.copy();
    }

    public void setCursorLocation(Location location) {
        cursorLocation.setRow(location.getRow());
        cursorLocation.setColumn(location.getColumn());
    }

    public int getCursorSize() {
        return cursorSize;
    }

    public void setCursorSize(int cursorSize) {
        this.cursorSize = cursorSize;
    }

    public void moveCursorLeft() {
        if (lines.isEmpty()) return;
        int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        if (row == 0 && column < 0) return;
        if (column >= 0) {
            column--;
        } else {
            row--;
            column = lines.get(row).length() - 1;
        }
        Location location = new Location(row, column);
        notifyCursorObservers(obs -> obs.updateCursorLocation(location));
    }

    public void moveCursorRight() {
        if (lines.isEmpty()) return;
        int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        int size = lines.get(row).length() - 1;
        if (row == lines.size() - 1 && column == size) return;
        if (column < size) {
            column++;
        } else {
            row++;
            column = -1;
        }
        Location location = new Location(row, column);
        notifyCursorObservers(obs -> obs.updateCursorLocation(location));
    }

    public void moveCursorUp() {
        if (lines.isEmpty()) return;
        int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        if (row == 0) return;
        row--;
        int size = lines.get(row).length() - 1;
        if (column >= size) column = size;
        Location location = new Location(row, column);
        notifyCursorObservers(obs -> obs.updateCursorLocation(location));
    }

    public void moveCursorDown() {
        if (lines.isEmpty()) return;
        int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        if (row == lines.size() - 1) return;
        row++;
        int size = lines.get(row).length() - 1;
        if (column >= size) column = size;
        Location location = new Location(row, column);
        notifyCursorObservers(obs -> obs.updateCursorLocation(location));
    }

    public void moveCursorStart() {
        notifyCursorObservers(obs -> obs.updateCursorLocation(new Location(0, -1)));
    }

    public void moveCursorEnd() {
        int row = lines.size() - 1;
        String line = lines.get(row);
        int column = line.length() - 1;
        notifyCursorObservers(obs -> obs.updateCursorLocation(new Location(row, column)));
    }

    /* ------------------------------------ */

    /* ################################################ */

    /* Methods used for text manipulation */
    public List<String> insert(char c) {
        return insert(String.valueOf(c));
    }

    public List<String> insert(String text) {
        InsertAction a = new InsertAction(this, text);
        a.executeDo();
        manager.push(a);
        return null;
    }

    public List<String> insert(char[] data) {
        List<String> deleted = new ArrayList<>();
        if (lines.isEmpty()) {
            String line = String.valueOf(data);
            lines.add(line);
            Location l = new Location(0, line.length() - 1);
            notifyTextObservers(TextObserver::updateText);
            notifyCursorObservers(obs -> obs.updateCursorLocation(l));
            return deleted;
        }
        if (!selectionRange.equals(new LocationRange())) {
            deleted.addAll(selectedText(selectionRange, false));
        }
        int start = 0;
        int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        String line = lines.get(row);
        for (int i = 0; i < data.length; i++) {
            char c = data[i];
            if (c == '\n') {
                lines.set(row, line.substring(0, column + 1) + String.valueOf(data, start, i - start));
                row++;
                lines.add(row, line.substring(column + 1));
                column = -1;
                cursorLocation.setRow(row);
                cursorLocation.setColumn(column);
                start += i + 1;
            }
        }
        String newChar = String.valueOf(data, start, data.length - start);
        if (!newChar.isEmpty()) {
            lines.set(row, line.substring(0, column + 1) + newChar + line.substring(column + 1));
            cursorLocation.setColumn(column + newChar.length());
        }
        Location l = cursorLocation.copy();
        notifyTextObservers(TextObserver::updateText);
        notifyCursorObservers(obs -> obs.updateCursorLocation(l));
        return deleted;
    }

    public void deleteAfter() {
        /*int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        String line = lines.get(row);
        if (row == lines.size() - 1 && column == line.length() - 1) return;
        if (column == line.length() - 1) {
            lines.set(row, line + lines.remove(row + 1));
        } else {
            lines.set(row, line.substring(0, column + 1) + line.substring(column + 2));
        }*/
        EditAction a = new DeleteAfterAction(this);
        a.executeDo();
        manager.push(a);
        notifyTextObservers(TextObserver::updateText);
        notifyCursorObservers(obs -> obs.updateCursorLocation(cursorLocation));
    }

    /* ---------------------------------- */
    public void deleteBefore() {
        /*int row = cursorLocation.getRow();
        int column = cursorLocation.getColumn();
        if (row == 0 && column == -1) return;
        String line = lines.get(row);
        if (column == -1) {
            String lineBefore = lines.get(row - 1);
            lines.set(row - 1, lineBefore + lines.remove(row));
            cursorLocation.setRow(row - 1);
            cursorLocation.setColumn(lineBefore.length() - 1);
        } else {
            lines.set(row, line.substring(0, column) + line.substring(column + 1));
            cursorLocation.setColumn(column - 1);
        }*/
        EditAction a = new DeleteBeforeAction(this);
        a.executeDo();
        manager.push(a);
        notifyTextObservers(TextObserver::updateText);
        notifyCursorObservers(obs -> obs.updateCursorLocation(cursorLocation));
    }

    public List<String> deleteRange(LocationRange range) {
        //return deleteRange(range, true);
        EditAction a = new DeleteRangeAction(this, range);
        a.executeDo();
        manager.push(a);
        notifyTextObservers(TextObserver::updateText);
        notifyCursorObservers(obs -> obs.updateCursorLocation(cursorLocation));
        return null;
    }

    public List<String> selectedText(LocationRange range, boolean remove) {
        Location start = range.getStart();
        Location end = range.getEnd();
        int rowStart = start.getRow();
        int rowEnd = end.getRow();
        if (rowStart > rowEnd) {
            int temp = rowStart;
            rowStart = rowEnd;
            rowEnd = temp;
        }
        int columnStart = start.getColumn();
        int columnEnd = end.getColumn();
        if (columnStart > columnEnd) {
            int temp = columnStart;
            columnStart = columnEnd;
            columnEnd = temp;
        }
        List<String> selected = new ArrayList<>();
        if (rowStart == rowEnd) {
            String line = lines.get(rowStart);
            if (remove)
                lines.set(rowStart, line.substring(0, columnStart + 1) + line.substring(columnEnd + 1));
            selected.add(line.substring(columnStart + 1, columnEnd + 1));
        } else {
            List<Integer> rows = new ArrayList<>();
            for (int row = rowStart; row <= rowEnd; row++) {
                String line = lines.get(row);
                if (row == rowStart) {
                    if (remove)
                        lines.set(row, line.substring(0, columnStart + 1));
                    selected.add(line.substring(columnStart + 1));
                    if (lines.get(row).isEmpty()) rows.add(row);
                } else if (row == rowEnd) {
                    if (remove)
                        lines.set(row, line.substring(columnEnd + 1));
                    selected.add(line.substring(0, columnEnd + 1));
                    if (lines.get(row).isEmpty()) rows.add(row);
                } else {
                    rows.add(row);
                    selected.add(line);
                }
            }
            if (remove) {
                for (int i = rows.size() - 1; i >= 0; i--) {
                    int row = rows.get(i);
                    lines.remove(row);
                }
            }
        }
        cursorLocation.setRow(rowStart);
        cursorLocation.setColumn(columnStart);
        return selected;
    }

    public LocationRange getSelectionRange() {
        return selectionRange.copy();
    }

    public void setSelectionRange(LocationRange range) {
        selectionRange.setStart(range.getStart());
        selectionRange.setEnd(range.getEnd());
        notifySelectionObservers(obs -> obs.selectionUpdated(selectionRange.equals(new LocationRange())
                ? SelectionState.EMPTY : SelectionState.NOT_EMPTY));
    }
    /* ################################################ */

    /* Methods for observer manipulation */
    public void attachCursorObserver(CursorObserver observer) {
        cursorObservers.add(observer);
    }

    public void detachCursorObserver(CursorObserver observer) {
        cursorObservers.remove(observer);
    }

    public void notifyCursorObservers(Consumer<CursorObserver> action) {
        cursorObservers.forEach(action);
    }

    public void attachTextObserver(TextObserver observer) {
        textObservers.add(observer);
    }

    public void detachTextObserver(TextObserver observer) {
        textObservers.remove(observer);
    }

    public void notifyTextObservers(Consumer<TextObserver> action) {
        textObservers.forEach(action);
    }

    public void attachSelectionObserver(SelectionObserver observer) {
        selectionObservers.add(observer);
    }

    public void detachSelectionObserver(SelectionObserver observer) {
        selectionObservers.remove(observer);
    }

    private void notifySelectionObservers(Consumer<SelectionObserver> action) {
        selectionObservers.forEach(action);
    }

    /* ---------------------------------- */

    /* ################################################ */

    /* Methods for iterating over list of lines */
    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = new CopyOnWriteArrayList<>(lines);
    }

    public Iterator<String> allLines() {
        return new MyIterator(lines);
    }

    public Iterator<String> linesRange(int from, int to) {
        return new MyIterator(lines, from, to);
    }

    private static class MyIterator implements Iterator<String> {
        private final List<String> lines;
        private final int to;
        private int currentIndex;

        private MyIterator(List<String> lines, int from, int to) {
            this.lines = lines;
            this.to = to;
            this.currentIndex = from;
        }

        private MyIterator(List<String> lines) {
            this(lines, 0, lines.size());
        }

        @Override
        public boolean hasNext() {
            return currentIndex != to;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return lines.get(currentIndex++);
        }
    }
    /* --------------------------------------------- */

}
