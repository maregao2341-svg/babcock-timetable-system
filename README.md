# SCHOOL PROJECT DOCUMENTATION

## Title
Design and Implementation of a Course–Venue–Lecturer Timetable Management System Using Java: A Babcock University Case Study

**Student:** [Your name]  
**Matric number:** [Your number]  
**Department:** [Your department]  
**Course:** [Course code/title]  
**Supervisor:** [Supervisor]  
**Date:** [Date]

## Abstract
This project presents a simplified desktop timetable management application developed with Java Swing. It records lecture schedules containing course, lecturer, venue, student group, day, time and duration. The program validates proposed allocations to prevent simultaneous bookings of a lecturer, venue or student group. It stores data in CSV format and exports timetable reports to Excel and PDF. The application was developed as an independent academic demonstration and uses fictional sample allocations. It does not connect to official university systems.

## Chapter 1 — Introduction
### Background
Manual timetable preparation often involves checking multiple courses, room allocations and staff availability. An electronic tool can improve consistency by checking scheduling rules at the moment an allocation is entered.
### Problem statement
Manual timetable updates may cause double-booking of rooms or lecturers and clashes between courses for a student group.
### Aim
To develop a Java desktop application for recording, checking and presenting course–venue–lecturer allocations.
### Objectives
1. Implement an easy-to-use graphical interface.
2. Allow users to add, update and delete scheduled lectures.
3. Detect overlapping venue, lecturer and student-group allocations.
4. Store timetable records and reload them on startup.
5. Export reports in Excel and PDF formats.
### Scope
Weekly Monday–Friday lecture scheduling with manually entered allocations and demonstration records.
### Limitations
No online multiuser access, login, automatic optimal scheduler, capacity constraint, database, or live university synchronization.

## Chapter 2 — Background and tools
**Java:** language used for application logic.  
**Swing/AWT:** graphical widgets, window layout and event handling.  
**Collections:** manage timetable lesson objects.  
**CSV:** lightweight file storage.  
**XML/ZIP:** create an XLSX workbook without external dependencies.  
**PDF output:** generated using Java's built-in file output.  
Compare manual scheduling with a GUI-assisted timetable in your own words, citing sources used.

## Chapter 3 — System design
### Actors
Timetable administrator / person entering schedules.
### Functional requirements
- View timetable by day and venue.
- Add/update/delete a scheduled lesson.
- Check timetable clashes before saving.
- Persist information and export files.
### Nonfunctional requirements
- Run with JDK 17+ on a supported desktop operating system.
- Provide readable GUI and informative validation messages.
- Use no third-party runtime dependencies.
### Data model
`Lesson(code, title, lecturer, venue, group, day, start, duration)`.
### Workflow
User enters lecture → app validates fields and allowed hours → scans existing lessons for overlaps → rejects conflicts or accepts the lesson → saves CSV → refreshes tables.
### Scheduling rule
Two lessons overlap when `candidate.start < other.end()` and `other.start < candidate.end()` on the same day. If they also share a venue, lecturer, or group, the proposed booking is rejected.

## Chapter 4 — Implementation and testing
### Files and main methods
- `BabcockTimetable.java`: full source.
- `Lesson`: a timetable record.
- `validation()`: checks input, schedule constraints and clashes.
- `addLesson()`, `updateLesson()`, `deleteLesson()`: CRUD actions.
- `refreshGrid()` / `refreshAll()`: display updates.
- `loadData()` / `saveData()`: CSV persistence.
- `writeExcel()` / `writePdf()`: exports.
- `main()`: initializes the application.
### Test cases
| Test | Expected result |
|---|---|
| Add valid non-overlapping class | Accepted |
| Add overlapping class in same venue | Rejected |
| Book a lecturer twice at overlapping times | Rejected |
| Book same student group twice | Rejected |
| Book lecture crossing midday break | Rejected |
| Book Friday afternoon | Rejected |
| Edit/delete an existing row | Updated/removed |
| Restart application | Saved data reloads |
| Export Excel | XLSX opens with timetable grid and list |
| Export PDF | PDF opens with daily schedule |

Run `java BabcockTimetable --test` and capture its actual output. Attach application screenshots, a rejected-clash dialog, and spreadsheet/PDF samples. Do not claim manual tests passed unless you performed them.

## Chapter 5 — Conclusion and recommendations
### Conclusion
The implemented system demonstrates a practical way to manage lecture allocations using a GUI, input validation, local storage and document exports. It can reduce avoidable manual clashes within the project rules.
### Recommendations
Implement an automatic scheduler, database, staff availability preferences, room capacity rules and user authentication in a future version.

## References
Add your institution's academic timetabling requirements (if supplied), official Java documentation, and any other sources you consulted. Include author/organization, title, publication year or access date and URL according to your course's required citation style.

## Screenshots / appendices
- Figure 1: Main application screen.
- Figure 2: Detailed timetable list.
- Figure 3: Conflict warning.
- Figure 4: Excel output.
- Figure 5: PDF output.
- Appendix A: Source code / GitHub repository URL.
