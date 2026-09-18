part of 'main.dart';

class StudentAssignmentsScreen extends StatefulWidget {
  const StudentAssignmentsScreen({super.key});
  @override State<StudentAssignmentsScreen> createState() => _StudentAssignmentsState();
}
class _StudentAssignmentsState extends State<StudentAssignmentsScreen> {
  bool loading = true;
  List<Map<String,dynamic>> rows = [];
  @override void initState() { super.initState(); load(); }
  Future<void> load() async {
    setState(() => loading = true);
    try { rows = await ApiService.instance.studentAssignments(); }
    catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => loading = false); }
  }
  Future<void> submit(Map<String,dynamic> x) async {
    final c = TextEditingController();
    final result = await showDialog<String>(context: context, builder: (d) => AlertDialog(
      title: const Text('Submit assignment'),
      content: TextField(controller: c, keyboardType: TextInputType.url,
        decoration: const InputDecoration(labelText: 'Submission URL')),
      actions: [
        TextButton(onPressed: () => Navigator.pop(d), child: const Text('Cancel')),
        FilledButton(onPressed: () => Navigator.pop(d, c.text.trim()), child: const Text('Submit')),
      ],
    ));
    if (result == null || result.isEmpty) { c.dispose(); return; }
    try {
      await ApiService.instance.mapPost('/submissions', {'assignmentId': x['assignmentId'], 'submissionUrl': result});
      if (mounted) snack(context, 'Assignment submitted successfully.');
      await load();
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    c.dispose();
  }
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('My Assignments'), actions: [IconButton(onPressed: load, icon: const Icon(Icons.refresh))]),
    body: loading ? const Center(child: CircularProgressIndicator()) : RefreshIndicator(
      onRefresh: load,
      child: rows.isEmpty ? ListView(children: const [SizedBox(height: 180), Center(child: Text('No assignments found.'))]) :
      ListView.builder(padding: const EdgeInsets.all(16), itemCount: rows.length, itemBuilder: (c, i) {
        final x = rows[i];
        final status = (x['submissionStatus'] ?? 'PENDING').toString();
        return Card(child: ListTile(
          title: Text((x['title'] ?? 'Assignment').toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text((x['subjectName'] ?? 'Subject').toString() + ' · Due ' + (x['dueDate'] ?? '—').toString() + '\nStatus: ' + status),
          isThreeLine: true,
          trailing: FilledButton(onPressed: () => submit(x), child: Text(status == 'PENDING' ? 'Submit' : 'Update')),
        ));
      }),
    ),
  );
}

class TeacherAssignmentsScreen extends StatefulWidget {
  const TeacherAssignmentsScreen({super.key});
  @override State<TeacherAssignmentsScreen> createState() => _TeacherAssignmentsState();
}
class _TeacherAssignmentsState extends State<TeacherAssignmentsScreen> {
  bool loading = true;
  List<Map<String,dynamic>> rows = [];
  List<Map<String,dynamic>> classSubjects = [];
  @override void initState() { super.initState(); load(); }
  Future<List<Map<String,dynamic>>> loadSubjects() async {
    final classes = await ApiService.instance.list('/classes/mine');
    final all = <Map<String,dynamic>>[];
    for (final cls in classes) {
      final id = cls['id'];
      if (id == null) continue;
      final ss = await ApiService.instance.list('/classes/' + id.toString() + '/subjects');
      for (final s in ss) {
        final v = Map<String,dynamic>.from(s);
        v['className'] = cls['name'];
        all.add(v);
      }
    }
    return all;
  }
  Future<void> load() async {
    setState(() => loading = true);
    try {
      final r = await Future.wait([ApiService.instance.teacherAssignments(), loadSubjects()]);
      rows = r[0] as List<Map<String,dynamic>>;
      classSubjects = r[1] as List<Map<String,dynamic>>;
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => loading = false); }
  }
  Future<void> createAssignment() async {
    if (classSubjects.isEmpty) { snack(context, 'No class subjects are assigned to you.'); return; }
    int? subjectId = int.tryParse(classSubjects.first['id'].toString());
    final title = TextEditingController();
    final description = TextEditingController();
    final due = TextEditingController();
    final max = TextEditingController(text: '100');
    final payload = await showDialog<Map<String,dynamic>>(context: context, builder: (d) => StatefulBuilder(
      builder: (d, set) => AlertDialog(
        title: const Text('Create assignment'),
        content: SingleChildScrollView(child: Column(children: [
          DropdownButtonFormField<int>(
            value: subjectId,
            items: classSubjects.map((s) => DropdownMenuItem<int>(
              value: int.tryParse(s['id'].toString()),
              child: Text((s['className'] ?? 'Class').toString() + ' · ' + (s['subjectName'] ?? s['subjectCode'] ?? 'Subject').toString()),
            )).toList(),
            onChanged: (v) => set(() => subjectId = v),
            decoration: const InputDecoration(labelText: 'Class / subject'),
          ),
          TextField(controller: title, decoration: const InputDecoration(labelText: 'Title')),
          TextField(controller: description, maxLines: 3, decoration: const InputDecoration(labelText: 'Description')),
          TextField(controller: due, decoration: const InputDecoration(labelText: 'Due date (YYYY-MM-DD)')),
          TextField(controller: max, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Maximum marks')),
        ])),
        actions: [
          TextButton(onPressed: () => Navigator.pop(d), child: const Text('Cancel')),
          FilledButton(onPressed: () {
            if (subjectId == null || title.text.trim().isEmpty || due.text.trim().isEmpty) return;
            Navigator.pop(d, {'classSubjectId': subjectId, 'title': title.text.trim(), 'description': description.text.trim(), 'dueDate': due.text.trim(), 'maxMarks': int.tryParse(max.text.trim()) ?? 100});
          }, child: const Text('Create')),
        ],
      ),
    ));
    title.dispose(); description.dispose(); due.dispose(); max.dispose();
    if (payload == null) return;
    try { await ApiService.instance.mapPost('/assignments', payload); if (mounted) snack(context, 'Assignment created.'); await load(); }
    catch (e) { if (mounted) snack(context, cleanError(e)); }
  }
  Future<void> submissions(Map<String,dynamic> x) async {
    final id = x['assignmentId'] ?? x['id'];
    if (id == null) return;
    try {
      final r = await ApiService.instance.list('/submissions/assignment/' + id.toString());
      if (!mounted) return;
      Navigator.push(context, MaterialPageRoute(builder: (_) => AssignmentSubmissionsScreen(assignment: x, rows: r)));
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
  }
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('My Assignments'), actions: [IconButton(onPressed: load, icon: const Icon(Icons.refresh))]),
    floatingActionButton: FloatingActionButton.extended(onPressed: createAssignment, icon: const Icon(Icons.add), label: const Text('Create')),
    body: loading ? const Center(child: CircularProgressIndicator()) : RefreshIndicator(onRefresh: load,
      child: rows.isEmpty ? ListView(children: const [SizedBox(height: 180), Center(child: Text('No assignments found.'))]) :
      ListView.builder(padding: const EdgeInsets.fromLTRB(16,16,16,100), itemCount: rows.length, itemBuilder: (c,i) {
        final x = rows[i];
        return Card(child: ListTile(
          leading: const CircleAvatar(child: Icon(Icons.assignment_outlined)),
          title: Text((x['title'] ?? 'Assignment').toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text((x['subjectName'] ?? 'Subject').toString() + ' · Due ' + (x['dueDate'] ?? '—').toString() + '\n' + (x['pendingReviewCount'] ?? 0).toString() + ' pending reviews'),
          isThreeLine: true, trailing: const Icon(Icons.chevron_right), onTap: () => submissions(x),
        ));
      }),
    ),
  );
}

class AssignmentSubmissionsScreen extends StatefulWidget {
  final Map<String,dynamic> assignment;
  final List<Map<String,dynamic>> rows;
  const AssignmentSubmissionsScreen({super.key, required this.assignment, required this.rows});
  @override State<AssignmentSubmissionsScreen> createState() => _AssignmentSubmissionsState();
}
class _AssignmentSubmissionsState extends State<AssignmentSubmissionsScreen> {
  late List<Map<String,dynamic>> rows;
  @override void initState() { super.initState(); rows = widget.rows.map((x) => Map<String,dynamic>.from(x)).toList(); }
  Future<void> evaluate(Map<String,dynamic> x) async {
    final marks = TextEditingController(text: (x['marks'] ?? '').toString());
    final feedback = TextEditingController(text: (x['feedback'] ?? '').toString());
    final r = await showDialog<Map<String,dynamic>>(context: context, builder: (d) => AlertDialog(
      title: Text((x['studentName'] ?? 'Student').toString()),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: marks, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Marks')),
        TextField(controller: feedback, maxLines: 3, decoration: const InputDecoration(labelText: 'Feedback')),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(d), child: const Text('Cancel')),
        FilledButton(onPressed: () => Navigator.pop(d, {'marks': int.tryParse(marks.text.trim()), 'feedback': feedback.text.trim()}), child: const Text('Save')),
      ],
    ));
    if (r == null || r['marks'] == null) { marks.dispose(); feedback.dispose(); return; }
    try {
      final updated = await ApiService.instance.request('/submissions/' + x['id'].toString() + '/evaluate?marks=' + Uri.encodeQueryComponent(r['marks'].toString()) + '&feedback=' + Uri.encodeQueryComponent(r['feedback'].toString()), method: 'PUT');
      final i = rows.indexWhere((a) => a['id'].toString() == x['id'].toString());
      if (i >= 0) setState(() => rows[i] = updated);
      if (mounted) snack(context, 'Submission evaluated.');
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    marks.dispose(); feedback.dispose();
  }
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text((widget.assignment['title'] ?? 'Submissions').toString())),
    body: rows.isEmpty ? const Center(child: Text('No submissions yet.')) : ListView.builder(
      padding: const EdgeInsets.all(16), itemCount: rows.length, itemBuilder: (c,i) {
        final x = rows[i];
        return Card(child: ListTile(
          title: Text((x['studentName'] ?? 'Student').toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
          subtitle: Text((x['status'] ?? 'SUBMITTED').toString() + ' · ' + (x['submittedAt'] ?? '').toString() + '\n' + (x['submissionUrl'] ?? '').toString() + '\nMarks: ' + (x['marks'] ?? '—').toString()),
          isThreeLine: true,
          trailing: IconButton(onPressed: () => evaluate(x), icon: const Icon(Icons.rate_review_outlined)),
        ));
      },
    ),
  );
}
