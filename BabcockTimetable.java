import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/**
 * BABCOCK UNIVERSITY-STYLE COURSE / VENUE / LECTURER TIMETABLE
 * Simplified one-file Java Swing school project. No external libraries.
 * Demo data are fictional; this is not an official Babcock University system.
 * Compile: javac BabcockTimetable.java
 * Run:     java BabcockTimetable
 */
public class BabcockTimetable extends JFrame {
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final String[] HEADERS = {"Day", "Start", "End", "Course", "Title", "Lecturer", "Venue", "Group"};
    private static final Path DATA = Paths.get("babcock_timetable_data.csv");
    private static final List<Lesson> lessons = new ArrayList<>();

    // Every scheduled lesson has one course, one lecturer, one venue, and one student group.
    private static class Lesson {
        String code, title, lecturer, venue, group;
        int day, start, duration;
        Lesson(String c, String t, String l, String v, String g, int d, int s, int n) {
            code=c; title=t; lecturer=l; venue=v; group=g;
            day=d; start=s; duration=n;
        }
        int end() { return start + duration; }
    }

    private final DefaultTableModel listModel = new DefaultTableModel(HEADERS, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final DefaultTableModel gridModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable listTable = new JTable(listModel);
    private final JTable gridTable = new JTable(gridModel);
    private final JComboBox<String> gridDay = new JComboBox<>(DAYS);
    private final JTextField code = new JTextField(), title = new JTextField();
    private final JTextField lecturer = new JTextField(), venue = new JTextField(), group = new JTextField();
    private final JComboBox<String> day = new JComboBox<>(DAYS);
    private final JComboBox<String> start = new JComboBox<>();
    private final JComboBox<String> duration = new JComboBox<>(new String[]{"1 hour", "2 hours", "3 hours"});
    private final JLabel status = new JLabel(" ");
    private final List<Lesson> visible = new ArrayList<>();
    private Lesson selected;

    public BabcockTimetable() {
        super("Babcock University-Style Timetable System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1230, 790);
        setMinimumSize(new Dimension(900, 660));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(238, 243, 248));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 58, 88));
        header.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JLabel heading = new JLabel("BABCOCK UNIVERSITY  |  TIMETABLE MANAGER");
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitle = new JLabel("Course  •  Venue  •  Lecturer   |   Independent school-project demonstration");
        subtitle.setForeground(new Color(204, 223, 239));
        header.add(heading, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Two ways to view the same schedule: a weekly lecture grid and a detailed list.
        JTabbedPane tabs = new JTabbedPane();
        JPanel gridPanel = new JPanel(new BorderLayout(6, 6));
        JPanel dayBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dayBar.add(new JLabel("Show timetable for: "));
        dayBar.add(gridDay);
        dayBar.add(new JLabel("   13:00–14:00 break  |  No Friday afternoon lectures"));
        gridPanel.add(dayBar, BorderLayout.NORTH);
        gridTable.setRowHeight(65);
        gridTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        gridTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        gridTable.getTableHeader().setReorderingAllowed(false);
        gridTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setVerticalAlignment(SwingConstants.CENTER);
                String v = value == null ? "" : value.toString();
                if (col == 0) { setBackground(new Color(227, 237, 246)); setFont(getFont().deriveFont(Font.BOLD)); }
                else if (v.equals("BREAK")) setBackground(new Color(255, 235, 187));
                else if (v.equals("CLOSED")) setBackground(new Color(239, 241, 244));
                else if (!v.isEmpty()) setBackground(new Color(219, 242, 233));
                else setBackground(Color.WHITE);
                if (isSelected) setBackground(new Color(185, 216, 244));
                setForeground(new Color(29, 55, 73));
                return this;
            }
        });
        gridPanel.add(new JScrollPane(gridTable), BorderLayout.CENTER);
        tabs.addTab("Weekly timetable grid", gridPanel);

        listTable.setRowHeight(27);
        listTable.setAutoCreateRowSorter(true);
        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.getTableHeader().setReorderingAllowed(false);
        tabs.addTab("All scheduled classes", new JScrollPane(listTable));
        listTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listTable.getSelectedRow() >= 0) {
                int idx = listTable.convertRowIndexToModel(listTable.getSelectedRow());
                if (idx < visible.size()) showFields(visible.get(idx));
            }
        });
        gridDay.addActionListener(e -> refreshGrid());
        add(tabs, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.setBackground(new Color(238, 243, 248));
        lower.setBorder(BorderFactory.createEmptyBorder(2, 10, 9, 10));
        JPanel form = new JPanel(new GridLayout(2, 4, 10, 8));
        form.add(field("Course code", code));
        form.add(field("Course title", title));
        form.add(field("Lecturer", lecturer));
        form.add(field("Venue", venue));
        form.add(field("Student group (e.g. CSC 200L A)", group));
        form.add(field("Day", day));
        for (int h=7;h<=17;h++) start.addItem(String.format("%02d:00", h));
        form.add(field("Start time", start));
        form.add(field("Duration", duration));
        lower.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 2));
        JButton add = new JButton("Add class"), update = new JButton("Update selected");
        JButton delete = new JButton("Delete selected"), clear = new JButton("Clear form");
        JButton excel = new JButton("Export Excel (.xlsx)"), pdf = new JButton("Export PDF");
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear);
        buttons.add(excel); buttons.add(pdf);
        add.addActionListener(e -> addLesson());
        update.addActionListener(e -> updateLesson());
        delete.addActionListener(e -> deleteLesson());
        clear.addActionListener(e -> clearFields());
        excel.addActionListener(e -> exportFile(true));
        pdf.addActionListener(e -> exportFile(false));
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(buttons, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        lower.add(footer, BorderLayout.SOUTH);
        add(lower, BorderLayout.SOUTH);
        refreshAll();
    }

    private JPanel field(String name, JComponent widget) {
        JPanel p = new JPanel(new BorderLayout(3, 3));
        p.setOpaque(false);
        JLabel caption = new JLabel(name);
        caption.setFont(new Font("SansSerif", Font.BOLD, 11));
        p.add(caption, BorderLayout.NORTH);
        p.add(widget, BorderLayout.CENTER);
        return p;
    }
    private void showFields(Lesson l) {
        selected = l;
        code.setText(l.code); title.setText(l.title); lecturer.setText(l.lecturer);
        venue.setText(l.venue); group.setText(l.group);
        day.setSelectedIndex(l.day); start.setSelectedIndex(l.start-7);
        duration.setSelectedIndex(l.duration-1);
    }
    private void clearFields() {
        selected = null;
        code.setText(""); title.setText(""); lecturer.setText("");
        venue.setText(""); group.setText(""); day.setSelectedIndex(0);
        start.setSelectedIndex(0); duration.setSelectedIndex(0);
        listTable.clearSelection();
    }
    private Lesson readFields() {
        return new Lesson(code.getText().trim(), title.getText().trim(),
            lecturer.getText().trim(), venue.getText().trim(), group.getText().trim(),
            day.getSelectedIndex(), start.getSelectedIndex()+7, duration.getSelectedIndex()+1);
    }
    private static String validation(Lesson candidate, Lesson ignored) {
        if (candidate.code.isBlank() || candidate.title.isBlank() || candidate.lecturer.isBlank()
                || candidate.venue.isBlank() || candidate.group.isBlank()) return "Please fill in every field.";
        if (candidate.day<0 || candidate.day>4 || candidate.start<7 || candidate.duration<1 || candidate.duration>3)
            return "Invalid day or teaching period.";
        if (candidate.end()>18 || (candidate.start<14 && candidate.end()>13)
                || (candidate.day==2 && candidate.end()>17)
                || (candidate.day==4 && candidate.end()>13))
            return "Invalid lecture time: keep the 13:00–14:00 break; Wednesday ends 17:00; Friday ends 13:00.";
        for (Lesson other : lessons) {
            if (other==ignored || other.day!=candidate.day
                || candidate.start>=other.end() || other.start>=candidate.end()) continue;
            if (other.venue.equalsIgnoreCase(candidate.venue))
                return "Venue conflict: " + other.venue + " already has " + other.code + ".";
            if (other.lecturer.equalsIgnoreCase(candidate.lecturer))
                return "Lecturer conflict: " + other.lecturer + " already teaches " + other.code + ".";
            if (other.group.equalsIgnoreCase(candidate.group))
                return "Student group conflict: " + other.group + " already has " + other.code + ".";
        }
        return null;
    }
    private void addLesson() {
        Lesson l=readFields(); String error=validation(l,null);
        if (error!=null) { warn(error); return; }
        lessons.add(l); changed("Class added successfully."); clearFields();
    }
    private void updateLesson() {
        if (selected==null) { warn("Select a class from 'All scheduled classes' first."); return; }
        Lesson updated=readFields(); String error=validation(updated,selected);
        if (error!=null) { warn(error); return; }
        lessons.set(lessons.indexOf(selected),updated);
        changed("Class updated successfully."); clearFields();
    }
    private void deleteLesson() {
        if (selected==null) { warn("Select a class from 'All scheduled classes' first."); return; }
        if (JOptionPane.showConfirmDialog(this,"Delete " + selected.code + " from the timetable?",
             "Confirm delete",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        lessons.remove(selected); changed("Class deleted."); clearFields();
    }
    private void changed(String message) {
        try { saveData(); refreshAll(); status.setText(message + "  Changes saved to " + DATA); }
        catch (IOException ex) { warn("Could not save data: " + ex.getMessage()); refreshAll(); }
    }
    private void warn(String message) { JOptionPane.showMessageDialog(this,message,"Timetable",JOptionPane.WARNING_MESSAGE); }
    private static List<Lesson> ordered() {
        List<Lesson> list=new ArrayList<>(lessons);
        list.sort(Comparator.comparingInt((Lesson l)->l.day).thenComparingInt(l->l.start)
                    .thenComparing(l->l.venue,String.CASE_INSENSITIVE_ORDER));
        return list;
    }
    private void refreshAll() {
        visible.clear(); listModel.setRowCount(0);
        for (Lesson l : ordered()) {
            visible.add(l);
            listModel.addRow(new Object[]{DAYS[l.day],clock(l.start),clock(l.end()),l.code,
                l.title,l.lecturer,l.venue,l.group});
        }
        refreshGrid();
        status.setText(lessons.size()+" scheduled classes. Data saves automatically to " + DATA.toAbsolutePath());
    }
    private void refreshGrid() {
        int d=gridDay.getSelectedIndex(); if (d<0) d=0;
        String[] names=new String[12]; names[0]="VENUE";
        for (int h=7;h<=17;h++) names[h-6]=clock(h);
        gridModel.setDataVector(new Object[0][0],names);
        TreeSet<String> venues=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Lesson l:lessons) venues.add(l.venue);
        for (String venueName:venues) {
            Object[] row=new Object[12]; row[0]=venueName;
            for (int h=7;h<=17;h++) {
                if(h==13) row[h-6]="BREAK";
                else if(d==4 && h>=13 || d==2 && h>=17) row[h-6]="CLOSED";
                else {
                    Lesson found=null;
                    for (Lesson l:lessons) if(l.day==d && l.venue.equalsIgnoreCase(venueName)
                          && h>=l.start && h<l.end()) { found=l; break; }
                    row[h-6]=found==null?"":found.code+" / "+found.group+" / "+found.lecturer;
                }
            }
            gridModel.addRow(row);
        }
        if (gridTable.getColumnModel().getColumnCount()==12)
            for(int c=0;c<12;c++) gridTable.getColumnModel().getColumn(c).setPreferredWidth(c==0?170:155);
    }
    private static String clock(int h) { return String.format(Locale.ROOT,"%02d:00",h); }

    // Local CSV storage, created automatically on the first launch.
    private static String csv(String text) { return "\""+text.replace("\"","\"\"")+"\""; }
    private static List<String> parseCsv(String line) {
        List<String> out=new ArrayList<>(); StringBuilder part=new StringBuilder(); boolean quoted=false;
        for(int i=0;i<line.length();i++) {
            char ch=line.charAt(i);
            if(ch=='\"') { if(quoted && i+1<line.length() && line.charAt(i+1)=='\"') {part.append('"'); i++;} else quoted=!quoted; }
            else if(ch==',' && !quoted) {out.add(part.toString());part.setLength(0);} else part.append(ch);
        }
        out.add(part.toString()); return out;
    }
    private static void saveData() throws IOException {
        List<String> rows=new ArrayList<>(); rows.add("Code,Title,Lecturer,Venue,Group,Day,Start,Duration");
        for(Lesson l:ordered()) rows.add(String.join(",", csv(l.code),csv(l.title),csv(l.lecturer),
            csv(l.venue),csv(l.group),""+l.day,""+l.start,""+l.duration));
        Files.write(DATA,rows,StandardCharsets.UTF_8);
    }
    private static void loadData() throws IOException {
        lessons.clear();
        if(!Files.exists(DATA)) { seedData(); saveData(); return; }
        List<String> rows=Files.readAllLines(DATA,StandardCharsets.UTF_8);
        for(int i=1;i<rows.size();i++) {
            if(rows.get(i).isBlank())continue;
            List<String> fields=parseCsv(rows.get(i));
            if(fields.size()!=8) throw new IOException("Invalid CSV row " + (i+1));
            try { lessons.add(new Lesson(fields.get(0),fields.get(1),fields.get(2),fields.get(3),
                fields.get(4),Integer.parseInt(fields.get(5)),Integer.parseInt(fields.get(6)),
                Integer.parseInt(fields.get(7)))); }
            catch(NumberFormatException e) { throw new IOException("Invalid number on CSV row " + (i+1), e); }
        }
    }
    private static void seedData() {
        // Fictional lecturers and illustrative venue names, not actual university allocations.
        lessons.add(new Lesson("COS201","Object-Oriented Programming","Dr. Ada James","Hall A","CSC 200L A",0,8,2));
        lessons.add(new Lesson("COS203","Computer Architecture","Mr. Tunde Bello","Hall B","CSC 200L A",0,11,2));
        lessons.add(new Lesson("IFT310","Mobile App Development","Dr. Grace Okoro","ICT Lab","IFT 300L A",0,14,2));
        lessons.add(new Lesson("GEDS201","Entrepreneurship","Mrs. Ijeoma Cole","Hall A","BUS 200L A",1,9,2));
        lessons.add(new Lesson("COS205","Database Systems","Dr. Ada James","ICT Lab","CSC 200L A",1,14,2));
        lessons.add(new Lesson("COS201","Object-Oriented Programming","Dr. Ada James","Hall B","CSC 200L A",2,8,2));
        lessons.add(new Lesson("IFT310","Mobile App Development","Dr. Grace Okoro","ICT Lab","IFT 300L A",2,14,2));
        lessons.add(new Lesson("COS207","Operating Systems","Mr. Tunde Bello","Hall A","CSC 200L A",3,8,2));
        lessons.add(new Lesson("GEDS201","Entrepreneurship","Mrs. Ijeoma Cole","Hall B","BUS 200L A",3,11,2));
        lessons.add(new Lesson("COS209","Computer Networks","Dr. Grace Okoro","Hall A","CSC 200L A",4,9,2));
    }
    private void exportFile(boolean excel) {
        JFileChooser chooser=new JFileChooser();
        String ext=excel?".xlsx":".pdf";
        chooser.setSelectedFile(new File("Babcock_Timetable"+ext));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ext))
            path=Paths.get(path.toString()+ext);
        try {
            if(excel)writeExcel(path); else writePdf(path);
            JOptionPane.showMessageDialog(this,"Exported successfully:\n"+path.toAbsolutePath());
        } catch(Exception ex) { warn("Export failed: " + ex.getMessage()); }
    }

    // REAL .XLSX: two Excel worksheets written with built-in Java ZIP and XML APIs.
    private static void writeExcel(Path file) throws IOException {
        Path parent=file.toAbsolutePath().getParent(); if(parent!=null)Files.createDirectories(parent);
        List<List<String>> grid=new ArrayList<>();
        grid.add(List.of("BABCOCK UNIVERSITY-STYLE WEEKLY TIMETABLE"));
        grid.add(List.of("Independent school project. Lecturers and venues are fictional examples."));
        TreeSet<String> venues=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(Lesson l:lessons)venues.add(l.venue);
        for(int d=0;d<5;d++) {
            grid.add(List.of("")); grid.add(List.of(DAYS[d].toUpperCase(Locale.ROOT)));
            List<String> head=new ArrayList<>(); head.add("VENUE");
            for(int h=7;h<=17;h++)head.add(clock(h)); grid.add(head);
            for(String v:venues) {
                List<String> row=new ArrayList<>();row.add(v);
                for(int h=7;h<=17;h++) {
                    String text="";
                    if(h==13)text="BREAK";
                    else if(d==4&&h>=13 || d==2&&h>=17)text="CLOSED";
                    else for(Lesson l:lessons)if(l.day==d&&l.venue.equalsIgnoreCase(v)
                              &&h>=l.start&&h<l.end()) {text=l.code+"\n"+l.group+"\n"+l.lecturer;break;}
                    row.add(text);
                }
                grid.add(row);
            }
        }
        List<List<String>> list=new ArrayList<>();
        list.add(List.of("SCHEDULED CLASSES")); list.add(List.of("All weekly lessons, sorted by day and time"));
        list.add(Arrays.asList(HEADERS));
        for(Lesson l:ordered())list.add(Arrays.asList(DAYS[l.day],clock(l.start),clock(l.end()),
            l.code,l.title,l.lecturer,l.venue,l.group));
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(file))) {
            put(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                +"<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                +"<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                +"<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                +"<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                +"<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                +"<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                +"</Types>");
            put(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                +"<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                +"</Relationships>");
            put(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                +"<sheets><sheet name=\"Weekly Grid\" sheetId=\"1\" r:id=\"rId1\"/>"
                +"<sheet name=\"Scheduled Classes\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>");
            put(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                +"<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                +"<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                +"<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                +"</Relationships>");
            put(zip,"xl/styles.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                +"<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                +"<fonts count=\"3\"><font><sz val=\"10\"/><name val=\"Calibri\"/></font>"
                +"<font><b/><sz val=\"10\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                +"<font><b/><sz val=\"14\"/><color rgb=\"FF193A58\"/><name val=\"Calibri\"/></font></fonts>"
                +"<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
                +"<fill><patternFill patternType=\"gray125\"/></fill>"
                +"<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF193A58\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                +"<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                +"<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                +"<cellXfs count=\"3\"><xf xfId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"><alignment wrapText=\"1\" vertical=\"center\"/></xf>"
                +"<xf xfId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"
                +"<xf xfId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/></cellXfs>"
                +"<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                +"</styleSheet>");
            put(zip,"xl/worksheets/sheet1.xml",sheetXml(grid,true));
            put(zip,"xl/worksheets/sheet2.xml",sheetXml(list,false));
        }
    }
    private static String sheetXml(List<List<String>> rows,boolean grid) {
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            +"<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            +"<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews><cols>");
        int cols=grid?12:8;
        for(int c=1;c<=cols;c++) {
            int width=grid?(c==1?27:25):(c==5||c==6?30:19);
            b.append("<col min=\"").append(c).append("\" max=\"").append(c)
             .append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
        }
        b.append("</cols><sheetData>");
        for(int r=0;r<rows.size();r++) {
            List<String> row=rows.get(r);
            int style = r==0?2 : (grid && row.size()==12 && "VENUE".equals(row.get(0)))
                || (!grid && r==2)?1:0;
            if(grid && row.size()==1 && r>2 && !row.get(0).isEmpty())style=2;
            b.append("<row r=\"").append(r+1).append("\" ht=\"")
                .append(grid&&row.size()>1&&style==0?54:25).append("\" customHeight=\"1\">");
            for(int c=0;c<row.size();c++) {
                b.append("<c r=\"").append(column(c)).append(r+1).append("\" s=\"").append(style)
                 .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                 .append(escapeXml(row.get(c))).append("</t></is></c>");
            }
            b.append("</row>");
        }
        b.append("</sheetData><pageMargins left=\"0.25\" right=\"0.25\" top=\"0.5\" bottom=\"0.5\"/>");
        return b.append("</worksheet>").toString();
    }
    private static String column(int c) {
        StringBuilder b=new StringBuilder();
        for(int n=c+1;n>0;n=(n-1)/26)b.insert(0,(char)('A'+(n-1)%26));
        return b.toString();
    }
    private static String escapeXml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
            .replace("\"","&quot;").replace("'","&apos;");
    }
    private static void put(ZipOutputStream z,String path,String text) throws IOException {
        z.putNextEntry(new ZipEntry(path)); z.write(text.getBytes(StandardCharsets.UTF_8)); z.closeEntry();
    }

    // REAL PDF: basic PDF 1.4 writer; landscape pages, no external libraries.
    private static void writePdf(Path path) throws IOException {
        Path parent=path.toAbsolutePath().getParent(); if(parent!=null)Files.createDirectories(parent);
        List<String> pages=new ArrayList<>();
        for(int day=0;day<5;day++) {
            List<Lesson> onDay=new ArrayList<>();
            for(Lesson l:ordered())if(l.day==day)onDay.add(l);
            int pageCount=Math.max(1,(onDay.size()+17)/18);
            for(int p=0;p<pageCount;p++) {
                int from=p*18, to=Math.min(onDay.size(),from+18);
                pages.add(pdfDay(day,onDay.subList(from,to),p+1,pageCount));
            }
        }
        Map<Integer,byte[]> objects=new HashMap<>(); int count=4+pages.size()*2;
        objects.put(1,bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids=new StringBuilder();
        for(int i=0;i<pages.size();i++)kids.append(5+i*2).append(" 0 R ");
        objects.put(2,bytes("<< /Type /Pages /Kids [ "+kids+"] /Count "+pages.size()+" >>"));
        objects.put(3,bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.put(4,bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for(int i=0;i<pages.size();i++) {
            int pg=5+i*2; byte[] stream=bytes(pages.get(i));
            objects.put(pg,bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] "
                +"/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "+(pg+1)+" 0 R >>"));
            ByteArrayOutputStream b=new ByteArrayOutputStream();
            b.write(bytes("<< /Length "+stream.length+" >>\nstream\n")); b.write(stream);
            b.write(bytes("\nendstream")); objects.put(pg+1,b.toByteArray());
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(bytes("%PDF-1.4\n%Project\n"));
        List<Integer> offsets=new ArrayList<>(Collections.nCopies(count+1,0));
        for(int i=1;i<=count;i++) {
            offsets.set(i,out.size());out.write(bytes(i+" 0 obj\n"));
            out.write(objects.get(i));out.write(bytes("\nendobj\n"));
        }
        int xref=out.size();out.write(bytes("xref\n0 "+(count+1)+"\n0000000000 65535 f \n"));
        for(int i=1;i<=count;i++)out.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",offsets.get(i))));
        out.write(bytes("trailer\n<< /Size "+(count+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));
        Files.write(path,out.toByteArray());
    }
    private static String pdfDay(int d,List<Lesson> rows,int page,int pages) {
        StringBuilder b=new StringBuilder();
        box(b,0,513,842,82,.10,.23,.35);
        pdfText(b,"BABCOCK UNIVERSITY-STYLE TIMETABLE",25,555,19,true,1,1,1);
        pdfText(b,"COURSE / VENUE / LECTURER  |  INDEPENDENT SCHOOL PROJECT",25,529,10,false,.82,.88,.94);
        pdfText(b,DAYS[d].toUpperCase(Locale.ROOT)+(pages>1?" - PART "+page+" OF "+pages:""),25,480,17,true,.10,.23,.35);
        pdfText(b,"Babcock-style teaching hours: 07:00-13:00, break 13:00-14:00; no Friday afternoon classes.",25,457,9,false,.3,.4,.51);
        double[] widths={104,92,202,146,135,111};
        String[] names={"TIME","COURSE","COURSE TITLE","LECTURER","VENUE","GROUP"};
        box(b,25,415,790,28,.15,.35,.51);
        double x=25;
        for(int i=0;i<names.length;i++) { pdfText(b,names[i],x+7,424,9,true,1,1,1);x+=widths[i]; }
        if(rows.isEmpty())pdfText(b,"No classes scheduled for this day.",35,388,12,false,.25,.35,.45);
        for(int r=0;r<rows.size();r++) {
            Lesson l=rows.get(r);double y=415-(r+1)*21;
            if(r%2==0)box(b,25,y,790,21,.93,.96,.98);
            String[] vals={clock(l.start)+" - "+clock(l.end()),l.code,l.title,l.lecturer,l.venue,l.group};
            x=25;
            for(int i=0;i<vals.length;i++) {
                pdfText(b,shorten(vals[i],(int)(widths[i]/5.5)),x+7,y+6,9,i==1,.15,.24,.33);
                x+=widths[i];
            }
        }
        pdfText(b,"Demonstration data only - not an official university timetable.",25,32,9,false,.37,.46,.55);
        pdfText(b,"Generated from BabcockTimetable.java  |  "+lessons.size()+" scheduled lessons",475,32,9,false,.37,.46,.55);
        return b.toString();
    }
    private static void box(StringBuilder b,double x,double y,double w,double h,double r,double g,double bl) {
        b.append(String.format(Locale.ROOT,"%.3f %.3f %.3f rg %.1f %.1f %.1f %.1f re f\n",r,g,bl,x,y,w,h));
    }
    private static void pdfText(StringBuilder b,String s,double x,double y,int size,boolean bold,double r,double g,double bl) {
        String safe=s.replaceAll("[^\\x20-\\x7E]","?").replace("\\","\\\\")
                .replace("(","\\(").replace(")","\\)");
        b.append(String.format(Locale.ROOT,"%.3f %.3f %.3f rg BT /%s %d Tf %.1f %.1f Td (%s) Tj ET\n",
                r,g,bl,bold?"F2":"F1",size,x,y,safe));
    }
    private static String shorten(String s,int max) {return s.length()>max?s.substring(0,max-3)+"...":s;}
    private static byte[] bytes(String s) {return s.getBytes(StandardCharsets.ISO_8859_1);}

    // Built-in command-line test/export mode helps you demonstrate the project without opening the GUI.
    private static void selfTest() throws IOException {
        List<Lesson> original=new ArrayList<>(lessons);
        try {
            lessons.clear();
            Lesson a=new Lesson("COS100","Intro","Lecturer A","Hall A","Group A",0,8,2);
            lessons.add(a);
            check(validation(a,a)==null,"Valid lecture");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall A","Group B",0,9,1),null)!=null,"Room overlap");
            check(validation(new Lesson("COS101","Intro","Lecturer A","Hall B","Group B",0,9,1),null)!=null,"Lecturer overlap");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall B","Group A",0,9,1),null)!=null,"Group overlap");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall B","Group B",0,10,1),null)==null,"Nonoverlapping lecture");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall B","Group B",0,12,2),null)!=null,"Lunch break");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall B","Group B",4,14,1),null)!=null,"Friday afternoon");
            check(validation(new Lesson("COS101","Intro","Lecturer B","Hall B","Group B",2,16,2),null)!=null,"Wednesday closing");
            Path directory=Files.createTempDirectory("babcock-single-java-test-");
            Path excel=directory.resolve("timetable.xlsx"),pdf=directory.resolve("timetable.pdf");
            writeExcel(excel);writePdf(pdf);
            try(ZipFile z=new ZipFile(excel.toFile())) {
                check(z.getEntry("xl/worksheets/sheet1.xml")!=null,"Real XLSX with grid sheet");
                check(z.getEntry("xl/worksheets/sheet2.xml")!=null,"Real XLSX with list sheet");
            }
            check(Files.readString(pdf,StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"),"Real PDF file");
            System.out.println("ALL 11 TESTS PASSED");
        } finally { lessons.clear();lessons.addAll(original); }
    }
    private static void check(boolean pass,String name) {
        if(!pass)throw new IllegalStateException("FAILED: "+name);
        System.out.println("PASS: "+name);
    }
    public static void main(String[] args) {
        try {
            loadData();
            if(args.length>0 && args[0].equals("--test")) {selfTest();return;}
            if(args.length>0 && args[0].equals("--export")) {
                Path folder=args.length>1?Paths.get(args[1]):Paths.get(".");
                Files.createDirectories(folder);
                Path excel=folder.resolve("Babcock_Timetable.xlsx"), pdf=folder.resolve("Babcock_Timetable.pdf");
                writeExcel(excel);writePdf(pdf);
                System.out.println("Created: " + excel.toAbsolutePath());
                System.out.println("Created: " + pdf.toAbsolutePath());
                return;
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(()->new BabcockTimetable().setVisible(true));
        } catch(Exception ex) {
            System.err.println("Error: " + ex.getMessage());ex.printStackTrace();
            if(!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null,ex.getMessage(),
                "Timetable error",JOptionPane.ERROR_MESSAGE);
        }
    }
}
