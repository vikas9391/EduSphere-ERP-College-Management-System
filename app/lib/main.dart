import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

part 'profile_notifications.dart';
part 'auth_screens.dart';
part 'assignments.dart';
part 'student_exams.dart';
part 'teacher_exams.dart';
part 'admin_academics.dart';
part 'admin_people.dart';
part 'admin_classes.dart';
part 'admin_timetable.dart';
part 'admin_exams.dart';
part 'admin_exam_schedule.dart';
part 'admin_marks.dart';
part 'admin_class_subjects.dart';
part 'admin_users.dart';

const apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://localhost:8080/api');
const green = Color(0xFF2E7D32);

class ApiService {
  ApiService._();
  static final instance = ApiService._();
  String get base => apiUrl.replaceFirst(RegExp(r'/$'), '');

  Future<dynamic> request(String path, {String method = 'GET', Map<String, dynamic>? body, bool retry = true}) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken');
    final headers = <String, String>{'Accept': 'application/json', if (body != null) 'Content-Type': 'application/json', if (token?.isNotEmpty == true) 'Authorization': 'Bearer $token'};
    final uri = Uri.parse('$base$path');
    late http.Response response;
    final encoded = body == null ? null : jsonEncode(body);
    switch (method) {
      case 'POST': response = await http.post(uri, headers: headers, body: encoded); break;
      case 'PUT': response = await http.put(uri, headers: headers, body: encoded); break;
      case 'DELETE': response = await http.delete(uri, headers: headers); break;
      default: response = await http.get(uri, headers: headers);
    }
    if (response.statusCode == 401 && retry && !path.startsWith('/auth/')) {
      if (await refresh()) return request(path, method: method, body: body, retry: false);
    }
    dynamic data;
    try { data = response.body.isEmpty ? null : jsonDecode(response.body); } catch (_) { data = null; }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = data is Map ? (data['message'] ?? data['error'] ?? 'Request failed (${response.statusCode})') : 'Request failed (${response.statusCode})';
      throw Exception(message.toString());
    }
    return data is Map && data['data'] != null ? data['data'] : data;
  }

  Future<bool> refresh() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('refreshToken');
    if (token == null || token.isEmpty) return false;
    try {
      final response = await http.post(Uri.parse('$base/auth/refresh'), headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}, body: jsonEncode({'refreshToken': token}));
      if (response.statusCode < 200 || response.statusCode >= 300) throw Exception();
      final raw = jsonDecode(response.body);
      final data = raw is Map && raw['data'] is Map ? raw['data'] : raw;
      if (data is! Map || data['accessToken'] == null) throw Exception();
      await prefs.setString('accessToken', data['accessToken'].toString());
      if (data['refreshToken'] != null) await prefs.setString('refreshToken', data['refreshToken'].toString());
      if (data['role'] != null) await prefs.setString('role', data['role'].toString());
      return true;
    } catch (_) { await logout(); return false; }
  }

  Future<Map<String, dynamic>> login(String college, String email, String password) async {
    final raw = await request('/auth/login', method: 'POST', retry: false, body: {'collegeCode': college, 'email': email, 'password': password});
    final data = raw is Map ? Map<String, dynamic>.from(raw) : <String, dynamic>{};
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('accessToken', '${data['accessToken'] ?? ''}');
    await prefs.setString('refreshToken', '${data['refreshToken'] ?? ''}');
    await prefs.setString('email', '${data['email'] ?? email}');
    await prefs.setString('role', '${data['role'] ?? ''}');
    return data;
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('accessToken'); await prefs.remove('refreshToken'); await prefs.remove('role'); await prefs.remove('email');
  }

  Future<Map<String, dynamic>> map(String path) async { final v = await request(path); return v is Map ? Map<String, dynamic>.from(v) : {}; }
  Future<List<Map<String, dynamic>>> list(String path) async { final v = await request(path); return v is List ? v.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList() : []; }

  Future<Map<String, dynamic>> studentDashboard() => map('/student/dashboard');
  Future<Map<String, dynamic>> teacherDashboard() => map('/teacher/dashboard');
  Future<List<Map<String, dynamic>>> teacherStudents() => list('/teacher/students');
  Future<List<Map<String, dynamic>>> attendance() => list('/attendance');
  Future<Map<String, dynamic>> attendanceSummary() => map('/attendance/me/summary');
  Future<List<Map<String, dynamic>>> studentEnrollments() => list('/student/enrollments');
  Future<List<Map<String, dynamic>>> studentAssignments() => list('/student/assignments');
  Future<Map<String, dynamic>> studentResults() => map('/student/results');
  Future<Map<String, dynamic>> studentTimetable() => map('/student/timetable');
  Future<List<Map<String, dynamic>>> teacherAssignments() => list('/teacher/assignments');
  Future<List<Map<String, dynamic>>> myTimetable() => list('/timetable/mine');
  Future<Map<String, dynamic>> createAttendance(Map<String, dynamic> body) => mapPost('/attendance', body);
  Future<Map<String, dynamic>> updateAttendance(int id, Map<String, dynamic> body) => mapPut('/attendance/$id', body);
  Future<Map<String, dynamic>> mapPost(String path, Map<String, dynamic> body) async { final v = await request(path, method: 'POST', body: body); return v is Map ? Map<String, dynamic>.from(v) : {}; }
  Future<Map<String, dynamic>> mapPut(String path, Map<String, dynamic> body) async { final v = await request(path, method: 'PUT', body: body); return v is Map ? Map<String, dynamic>.from(v) : {}; }
}

void main() => runApp(const EduSphereApp());

class EduSphereApp extends StatelessWidget {
  const EduSphereApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'EduSphere ERP',
    theme: ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: green), scaffoldBackgroundColor: const Color(0xFFF6F7F2), inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: const Color(0xFFF0F4EE), border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none))),
    home: const RootScreen(),
  );
}

class RootScreen extends StatefulWidget { const RootScreen({super.key}); @override State<RootScreen> createState() => _RootState(); }
class _RootState extends State<RootScreen> {
  String? role; bool loading = true;
  @override void initState() { super.initState(); restore(); }
  Future<void> restore() async { final p = await SharedPreferences.getInstance(); final t = p.getString('accessToken'); if (mounted) setState(() { role = t?.isNotEmpty == true ? p.getString('role') : null; loading = false; }); }
  @override Widget build(BuildContext context) { if (loading) return const Scaffold(body: Center(child: CircularProgressIndicator())); if (role == null) return LoginScreen(onLogin: (r) => setState(() => role = r)); return HomeScreen(role: role!, onLogout: () async { await ApiService.instance.logout(); if (mounted) setState(() => role = null); }); }
}

class LoginScreen extends StatefulWidget { final void Function(String) onLogin; const LoginScreen({super.key, required this.onLogin}); @override State<LoginScreen> createState() => _LoginState(); }
class _LoginState extends State<LoginScreen> {
  final college = TextEditingController(), email = TextEditingController(), password = TextEditingController(); bool busy = false, obscure = true;
  Future<void> submit() async { if ([college, email, password].any((c) => c.text.trim().isEmpty)) { snack(context, 'Enter all login details.'); return; } setState(() => busy = true); try { final r = await ApiService.instance.login(college.text.trim(), email.text.trim(), password.text); widget.onLogin('${r['role'] ?? ''}'); } catch (e) { snack(context, cleanError(e)); } finally { if (mounted) setState(() => busy = false); } }
  @override Widget build(BuildContext context) => Scaffold(body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(24), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 480), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('EduSphere', style: TextStyle(fontSize: 34, fontWeight: FontWeight.w900, color: green)), const Text('College ERP mobile', style: TextStyle(color: Colors.black54)), const SizedBox(height: 34), Card(child: Padding(padding: const EdgeInsets.all(22), child: Column(children: [field('College code', college), field('Email', email), TextField(controller: password, obscureText: obscure, decoration: InputDecoration(labelText: 'Password', suffixIcon: IconButton(onPressed: () => setState(() => obscure = !obscure), icon: Icon(obscure ? Icons.visibility : Icons.visibility_off)))), const SizedBox(height: 22), SizedBox(width: double.infinity, height: 52, child: FilledButton(onPressed: busy ? null : submit, child: busy ? const CircularProgressIndicator(color: Colors.white) : const Text('Sign in'))),
const SizedBox(height: 8),
TextButton(onPressed: busy ? null : () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ForgotPasswordScreen())), child: const Text('Forgot password?'))])))])))));
  Widget field(String label, TextEditingController c) => Padding(padding: const EdgeInsets.only(bottom: 12), child: TextField(controller: c, decoration: InputDecoration(labelText: label)));
}

class HomeScreen extends StatefulWidget { final String role; final Future<void> Function() onLogout; const HomeScreen({super.key, required this.role, required this.onLogout}); @override State<HomeScreen> createState() => _HomeState(); }
class _HomeState extends State<HomeScreen> {
  Map<String, dynamic> data = {}; bool loading = true;
  String get normalizedRole => widget.role.toUpperCase();
  bool get teacher => normalizedRole.contains('TEACHER');
  bool get student => normalizedRole.contains('STUDENT');
  bool get admin => normalizedRole.contains('ADMIN') || normalizedRole.contains('SUPER_ADMIN');
  @override void initState() { super.initState(); load(); }
  Future<void> load() async { setState(() => loading = true); try { if (teacher) { data = await ApiService.instance.teacherDashboard(); } else if (student) { data = await ApiService.instance.studentDashboard(); } else { data = {}; } } catch (_) { data = {}; } finally { if (mounted) setState(() => loading = false); } }
  @override Widget build(BuildContext context) { final name = '${data['${teacher ? 'teacher' : 'student'}Name'] ?? (admin ? 'Administrator' : 'User')}';
    final items = teacher ? <_Module>[const _Module('Classes', Icons.school_outlined, TeacherClassesScreen()), const _Module('Attendance', Icons.fact_check_outlined, TeacherAttendanceScreen()), const _Module('Assignments', Icons.assignment_outlined, TeacherAssignmentsScreen()), const _Module('Timetable', Icons.calendar_month_outlined, TeacherTimetableScreen()), const _Module('Exams & Marks', Icons.event_note_outlined, TeacherExamsScreen()), const _Module('Change password', Icons.lock_reset, ChangePasswordScreen())] : student ? <_Module>[const _Module('Classes', Icons.school_outlined, StudentClassesScreen()), const _Module('Attendance', Icons.fact_check_outlined, StudentAttendanceScreen()), const _Module('Assignments', Icons.assignment_outlined, StudentAssignmentsScreen()), const _Module('Timetable', Icons.calendar_month_outlined, StudentTimetableScreen()), const _Module('Exams', Icons.event_note_outlined, StudentExamsScreen()), const _Module('Results', Icons.bar_chart_outlined, StudentResultsScreen()), const _Module('Profile', Icons.person_outline, ProfileScreen(role: 'STUDENT')), const _Module('Notifications', Icons.notifications_none_outlined, NotificationsScreen()), const _Module('Change password', Icons.lock_reset, ChangePasswordScreen())] : <_Module>[const _Module('Departments', Icons.account_tree_outlined, AdminResourceScreen(title: 'Departments', endpoint: '/departments', fields: ['code','hod'])), const _Module('Courses', Icons.menu_book_outlined, AdminResourceScreen(title: 'Courses', endpoint: '/courses', fields: ['courseCode','duration'])), const _Module('Subjects', Icons.subject_outlined, AdminResourceScreen(title: 'Subjects', endpoint: '/subjects', fields: ['subjectCode','credits','semester'])), const _Module('Teachers', Icons.badge_outlined, AdminPeopleListScreen(type: 'Teacher')), const _Module('Students', Icons.groups_outlined, AdminPeopleListScreen(type: 'Student')), const _Module('Classes', Icons.class_outlined, AdminClassesScreen()), const _Module('Announcements', Icons.campaign_outlined, AdminAnnouncementsScreen()), const _Module('User Management', Icons.manage_accounts_outlined, AdminUsersScreen()), const _Module('Timetable', Icons.calendar_month_outlined, AdminTimetableScreen()), const _Module('Exams & Schedules', Icons.event_note_outlined, AdminExamsScreen()), const _Module('Change password', Icons.lock_reset, ChangePasswordScreen())]; return Scaffold(appBar: AppBar(title: const Text('EduSphere', style: TextStyle(fontWeight: FontWeight.w900)), actions: [IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout))]), body: RefreshIndicator(onRefresh: load, child: ListView(padding: const EdgeInsets.all(20), children: [Text(teacher ? 'Welcome back,' : student ? 'Good to see you,' : 'Administration,', style: const TextStyle(color: Colors.black54)), Text(name, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900)), const SizedBox(height: 20), if (loading) const Center(child: Padding(padding: EdgeInsets.all(30), child: CircularProgressIndicator())) else ...[Container(padding: const EdgeInsets.all(20), decoration: BoxDecoration(color: green, borderRadius: BorderRadius.circular(22)), child: Text(teacher ? '${data['totalSubjects'] ?? 0} subjects · ${data['totalStudents'] ?? 0} students' : student ? '${data['course'] ?? 'Student'} · Attendance ${data['attendancePercentage'] ?? 0}%' : 'Administrative controls', style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w800))), const SizedBox(height: 20), const Text('Modules', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)), const SizedBox(height: 8), Card(child: Column(children: items.map((m) => ListTile(leading: CircleAvatar(backgroundColor: const Color(0xFFE8F5E9), child: Icon(m.icon, color: green)), title: Text(m.title, style: const TextStyle(fontWeight: FontWeight.w700)), trailing: const Icon(Icons.chevron_right), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => m.page))).toList()))]])); }
}
class _Module { final String title; final IconData icon; final Widget page; const _Module(this.title, this.icon, this.page); }

class StudentClassesScreen extends StatelessWidget { const StudentClassesScreen({super.key}); @override Widget build(BuildContext c) => DataListScreen(title: 'My Classes', future: ApiService.instance.studentEnrollments(), empty: 'No enrollments found.', item: (x) => '${x['subjectName'] ?? 'Subject'} · ${x['className'] ?? ''}', sub: (x) => '${x['subjectCode'] ?? ''} · ${x['teacherName'] ?? ''}'); }
class TeacherClassesScreen extends StatelessWidget { const TeacherClassesScreen({super.key}); @override Widget build(BuildContext c) => DataListScreen(title: 'My Classes', future: ApiService.instance.teacherStudents(), empty: 'No assigned students.', group: true, item: (x) => '${x['studentName'] ?? 'Student'}', sub: (x) => '${x['className'] ?? ''} · ${x['subjectName'] ?? ''} · ${x['admissionNo'] ?? ''}'); }

class StudentAttendanceScreen extends StatelessWidget { const StudentAttendanceScreen({super.key}); @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('My Attendance')), body: FutureBuilder(future: Future.wait([ApiService.instance.attendanceSummary(), ApiService.instance.attendance()]), builder: (c, s) { if (s.connectionState == ConnectionState.waiting) return const Center(child: CircularProgressIndicator()); if (s.hasError) return ErrorView('${s.error}'); final summary = s.data![0] as Map<String, dynamic>; final rows = s.data![1] as List<Map<String, dynamic>>; return ListView(padding: const EdgeInsets.all(20), children: [Card(child: Padding(padding: const EdgeInsets.all(22), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('Attendance percentage', style: TextStyle(color: Colors.black54)), const SizedBox(height: 6), Text('${summary['percentage'] ?? summary['attendancePercentage'] ?? 0}%', style: const TextStyle(fontSize: 36, fontWeight: FontWeight.w900, color: green))])), const SizedBox(height: 18), const Text('Recent records', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)), ...rows.take(30).map((x) => Card(child: ListTile(title: Text('${x['subjectName'] ?? 'Subject'}', style: const TextStyle(fontWeight: FontWeight.w700)), subtitle: Text('${x['attendanceDate'] ?? ''}'), trailing: Text('${x['status'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.w800)))))]); })); }

class TeacherAttendanceScreen extends StatefulWidget { const TeacherAttendanceScreen({super.key}); @override State<TeacherAttendanceScreen> createState() => _TeacherAttendanceState(); }
class _TeacherAttendanceState extends State<TeacherAttendanceScreen> {
  List<Map<String, dynamic>> students = [], records = []; String? group; DateTime date = DateTime.now(); final status = <String, String>{}; bool loading = true, saving = false;
  String get day => '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  @override void initState() { super.initState(); load(); }
  Future<void> load() async { setState(() => loading = true); try { final r = await Future.wait([ApiService.instance.teacherStudents(), ApiService.instance.attendance()]); students = r[0] as List<Map<String, dynamic>>; records = r[1] as List<Map<String, dynamic>>; final groups = _groups; group = groups.containsKey(group) ? group : groups.keys.firstOrNull; sync(); } catch (e) { if (mounted) snack(context, cleanError(e)); } finally { if (mounted) setState(() => loading = false); } }
  Map<String, List<Map<String, dynamic>>> get _groups { final g = <String, List<Map<String, dynamic>>>{}; for (final s in students) { final k = '${s['classSubjectId'] ?? ''}'; g.putIfAbsent(k, () => []).add(s); } return g; }
  List<Map<String, dynamic>> get selected => group == null ? [] : (_groups[group] ?? []);
  void sync() { status.clear(); for (final s in selected) { final id = '${s['id']}'; final r = records.where((x) => '${x['classEnrollmentId']}' == id && '${x['attendanceDate']}' == day).firstOrNull; status[id] = '${r?['status'] ?? 'PRESENT'}'.toUpperCase(); } }
  Future<void> save() async { setState(() => saving = true); try { for (final s in selected) { final eid = int.tryParse('${s['id']}'); if (eid == null) continue; final payload = {'classEnrollmentId': eid, 'attendanceDate': day, 'status': status['$eid'] ?? 'PRESENT'}; final old = records.where((x) => '${x['classEnrollmentId']}' == '$eid' && '${x['attendanceDate']}' == day).firstOrNull; if (old?['id'] != null) await ApiService.instance.updateAttendance(int.parse('${old!['id']}'), payload); else await ApiService.instance.createAttendance(payload); } snack(context, 'Attendance saved'); await load(); } catch (e) { snack(context, cleanError(e)); } finally { if (mounted) setState(() => saving = false); } }
  @override Widget build(BuildContext context) { final groups = _groups; return Scaffold(appBar: AppBar(title: const Text('Mark Attendance')), body: loading ? const Center(child: CircularProgressIndicator()) : Column(children: [Padding(padding: const EdgeInsets.all(16), child: Column(children: [DropdownButtonFormField<String>(value: group, decoration: const InputDecoration(labelText: 'Class / subject'), items: groups.entries.map((e) { final f=e.value.first; return DropdownMenuItem(value:e.key, child:Text('${f['className'] ?? 'Class'} · ${f['subjectName'] ?? 'Subject'}')); }).toList(), onChanged:(v){setState((){group=v;sync();});}), const SizedBox(height:10), ListTile(contentPadding:EdgeInsets.zero, title:Text('Date: $day'), trailing:const Icon(Icons.calendar_month), onTap:() async { final d=await showDatePicker(context:context, initialDate:date, firstDate:DateTime(2020), lastDate:DateTime(2100)); if(d!=null)setState((){date=d;sync();}); })])), Expanded(child:ListView.builder(padding:const EdgeInsets.fromLTRB(16,0,16,100), itemCount:selected.length, itemBuilder:(c,i){final s=selected[i]; final id='${s['id']}'; final value=status[id]??'PRESENT'; return Card(child:ListTile(title:Text('${s['studentName'] ?? 'Student'}'), subtitle:Text('${s['admissionNo'] ?? ''}'), trailing:DropdownButton<String>(value:value, items:['PRESENT','ABSENT','LATE','EXCUSED'].map((v)=>DropdownMenuItem(value:v,child:Text(v))).toList(), onChanged:(v)=>setState(()=>status[id]=v!)));})),]), floatingActionButton: FloatingActionButton.extended(onPressed:saving||selected.isEmpty?null:save,label:Text(saving?'Saving…':'Save'))); }
}

class StudentTimetableScreen extends StatelessWidget { const StudentTimetableScreen({super.key}); @override Widget build(BuildContext c) => TimetableScreen(title:'My Timetable', future:ApiService.instance.studentTimetable()); }
class TeacherTimetableScreen extends StatelessWidget { const TeacherTimetableScreen({super.key}); @override Widget build(BuildContext c) => DataListScreen(title:'My Timetable', future:ApiService.instance.myTimetable(), empty:'No timetable entries.', item:(x)=>'${x['subjectName'] ?? x['subject'] ?? 'Subject'}', sub:(x)=>'${x['day'] ?? ''} · ${x['startTime'] ?? ''}-${x['endTime'] ?? ''} · ${x['room'] ?? ''}'); }
class TimetableScreen extends StatelessWidget { final String title; final Future<Map<String,dynamic>> future; const TimetableScreen({super.key,required this.title,required this.future}); @override Widget build(BuildContext c)=>Scaffold(appBar:AppBar(title:Text(title)),body:FutureBuilder(future:future,builder:(c,s){if(s.connectionState==ConnectionState.waiting)return const Center(child:CircularProgressIndicator());if(s.hasError)return ErrorView('${s.error}');final d=s.data??{};final schedule=d['schedule'];if(schedule is! Map)return const Center(child:Text('No timetable available.'));return ListView(padding:const EdgeInsets.all(16),children:schedule.entries.expand<Widget>((e){final list=e.value is List?e.value as List:[];return [Padding(padding:const EdgeInsets.only(top:10,bottom:6),child:Text('${e.key}',style:const TextStyle(fontSize:18,fontWeight:FontWeight.w900))),...list.map((x)=>Card(child:ListTile(title:Text('${x['subjectName']??x['subject']??'Subject'}'),subtitle:Text('${x['startTime']??''}-${x['endTime']??''} · ${x['room']??''}'))))];}).toList());}));}

class StudentResultsScreen extends StatelessWidget { const StudentResultsScreen({super.key}); @override Widget build(BuildContext c)=>Scaffold(appBar:AppBar(title:const Text('My Results')),body:FutureBuilder(future:ApiService.instance.studentResults(),builder:(c,s){if(s.connectionState==ConnectionState.waiting)return const Center(child:CircularProgressIndicator());if(s.hasError)return ErrorView('${s.error}');final d=s.data??{};return ListView(padding:const EdgeInsets.all(20),children:[Card(child:Padding(padding:const EdgeInsets.all(22),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text('CGPA ${d['cgpa']??'—'}',style:const TextStyle(fontSize:32,fontWeight:FontWeight.w900,color:green)),Text('${d['overallResult']??'Result available'} · ${d['totalCredits']??0} credits',style:const TextStyle(color:Colors.black54))])),const SizedBox(height:18),if(d['subjects'] is List)...(d['subjects'] as List).map((x)=>Card(child:ListTile(title:Text('${x['subjectName']??x['subject']??'Subject'}'),trailing:Text('${x['marks']??x['grade']??'—'}',style:const TextStyle(fontWeight:FontWeight.w900)))))]); }));}

class DataListScreen extends StatelessWidget { final String title, empty; final Future<List<Map<String,dynamic>>> future; final String Function(Map<String,dynamic>) item, sub; final bool group; const DataListScreen({super.key,required this.title,required this.future,required this.empty,required this.item,required this.sub,this.group=false}); @override Widget build(BuildContext c)=>Scaffold(appBar:AppBar(title:Text(title)),body:FutureBuilder(future:future,builder:(c,s){if(s.connectionState==ConnectionState.waiting)return const Center(child:CircularProgressIndicator());if(s.hasError)return ErrorView('${s.error}');final rows=s.data??[];if(rows.isEmpty)return Center(child:Text(empty));return RefreshIndicator(onRefresh:()async{await future;},child:ListView.builder(padding:const EdgeInsets.all(16),itemCount:rows.length,itemBuilder:(c,i){final x=rows[i];return Card(child:ListTile(leading:const CircleAvatar(backgroundColor:Color(0xFFE8F5E9),child:Icon(Icons.menu_book,color:green)),title:Text(item(x),style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text(sub(x)));}));}));}
class ErrorView extends StatelessWidget { final String message; const ErrorView(this.message,{super.key}); @override Widget build(BuildContext c)=>Center(child:Padding(padding:const EdgeInsets.all(24),child:Text(message,textAlign:TextAlign.center))); }

String cleanError(Object e)=>e.toString().replaceFirst('Exception: ','');
void snack(BuildContext c,String m)=>ScaffoldMessenger.of(c).showSnackBar(SnackBar(content:Text(m),behavior:SnackBarBehavior.floating));

extension FirstOrNull<E> on Iterable<E> { E? get firstOrNull => isEmpty ? null : first; }
