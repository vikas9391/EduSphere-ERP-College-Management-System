import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

part 'auth_screens.dart';
part 'assignments.dart';

const apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://localhost:8080/api');

class ApiService {
  ApiService._();
  static final instance = ApiService._();
  String get base => apiUrl.replaceFirst(RegExp(r'/$'), '');

  Future<Map<String, dynamic>> _request(String path, {String method = 'GET', Map<String, dynamic>? body, bool retry = true}) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken');
    final headers = {'Accept': 'application/json', if (body != null) 'Content-Type': 'application/json', if (token != null) 'Authorization': 'Bearer $token'};
    http.Response response;
    final uri = Uri.parse('$base$path');
    if (method == 'POST') {
      response = await http.post(uri, headers: headers, body: body == null ? null : jsonEncode(body));
    } else {
      response = await http.get(uri, headers: headers);
    }
    if (response.statusCode == 401 && retry && !path.startsWith('/auth/')) {
      final refreshed = await refresh();
      if (refreshed) return _request(path, method: method, body: body, retry: false);
    }
    dynamic data;
    try { data = response.body.isEmpty ? null : jsonDecode(response.body); } catch (_) { data = null; }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception(data is Map ? (data['message'] ?? data['error'] ?? 'Request failed (${response.statusCode})') : 'Request failed (${response.statusCode})');
    }
    return (data is Map && data['data'] is Map) ? Map<String, dynamic>.from(data['data']) : (data is Map ? Map<String, dynamic>.from(data) : {});
  }

  Future<bool> refresh() async {
    final prefs = await SharedPreferences.getInstance();
    final refreshToken = prefs.getString('refreshToken');
    if (refreshToken == null) return false;
    try {
      final r = await http.post(Uri.parse('$base/auth/refresh'), headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}, body: jsonEncode({'refreshToken': refreshToken}));
      if (r.statusCode < 200 || r.statusCode >= 300) throw Exception();
      final raw = jsonDecode(r.body);
      final data = raw is Map && raw['data'] is Map ? raw['data'] : raw;
      if (data['accessToken'] == null) throw Exception();
      await prefs.setString('accessToken', data['accessToken']);
      if (data['refreshToken'] != null) await prefs.setString('refreshToken', data['refreshToken']);
      if (data['role'] != null) await prefs.setString('role', data['role']);
      return true;
    } catch (_) {
      await prefs.remove('accessToken'); await prefs.remove('refreshToken'); await prefs.remove('role'); await prefs.remove('username');
      return false;
    }
  }

  Future<Map<String, dynamic>> login(String collegeCode, String email, String password) async {
    final data = await _request('/auth/login', method: 'POST', body: {'collegeCode': collegeCode, 'email': email, 'password': password}, retry: false);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('accessToken', data['accessToken'] ?? '');
    await prefs.setString('email', data['email'] ?? email);
    await prefs.setBool('mustChangePassword', data['mustChangePassword'] == true);
    await prefs.setString('role', data['role'] ?? '');
    if (data['refreshToken'] != null) await prefs.setString('refreshToken', data['refreshToken']);
    return data;
  }

  Future<void> logout() async { final p = await SharedPreferences.getInstance(); await p.remove('accessToken'); await p.remove('refreshToken'); await p.remove('role'); await p.remove('email'); await p.remove('mustChangePassword'); }
  Future<Map<String, dynamic>> studentDashboard() => _request('/student/dashboard');
  Future<Map<String, dynamic>> teacherDashboard() => _request('/teacher/dashboard');
  Future<dynamic> request(String path, {String method = 'GET', Map<String,dynamic>? body, bool retry = true}) => _requestAny(path, method: method, body: body, retry: retry);
  Future<dynamic> _requestAny(String path, {String method='GET', Map<String,dynamic>? body, bool retry=true}) async {
    final prefs=await SharedPreferences.getInstance(); final token=prefs.getString('accessToken');
    final headers=<String,String>{'Accept':'application/json',if(body!=null)'Content-Type':'application/json',if(token?.isNotEmpty==true)'Authorization':'Bearer $token'};
    final uri=Uri.parse('$base$path'); http.Response response;
    if(method=='POST') response=await http.post(uri,headers:headers,body:body==null?null:jsonEncode(body));
    else if(method=='PUT') response=await http.put(uri,headers:headers,body:body==null?null:jsonEncode(body));
    else if(method=='DELETE') response=await http.delete(uri,headers:headers);
    else response=await http.get(uri,headers:headers);
    if(response.statusCode==401&&retry&&!path.startsWith('/auth/')){if(await refresh())return _requestAny(path,method:method,body:body,retry:false);}
    dynamic data;try{data=response.body.isEmpty?null:jsonDecode(response.body);}catch(_){data=null;}
    if(response.statusCode<200||response.statusCode>=300)throw Exception(data is Map?(data['message']??data['error']??'Request failed (${response.statusCode})'):'Request failed (${response.statusCode})');
    return data is Map&&data['data']!=null?data['data']:data;
  }
  Future<List<Map<String,dynamic>>> list(String path) async {final v=await request(path);return v is List?v.whereType<Map>().map((e)=>Map<String,dynamic>.from(e)).toList():[];}
  Future<Map<String,dynamic>> mapPost(String path,Map<String,dynamic> body)async{final v=await request(path,method:'POST',body:body);return v is Map?Map<String,dynamic>.from(v):{};}
  Future<Map<String,dynamic>> mapPut(String path,Map<String,dynamic> body)async{final v=await request(path,method:'PUT',body:body);return v is Map?Map<String,dynamic>.from(v):{};}
  Future<List<Map<String,dynamic>>> studentAssignments()=>list('/student/assignments');
  Future<List<Map<String,dynamic>>> teacherAssignments()=>list('/teacher/assignments');
}

void main() => runApp(const EduSphereApp());

class EduSphereApp extends StatelessWidget {
  const EduSphereApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'EduSphere ERP',
    theme: ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2E7D32), brightness: Brightness.light), scaffoldBackgroundColor: const Color(0xFFF6F7F2), inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: const Color(0xFFF0F4EE), border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none), enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFFE4E9E2))), focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFF2E7D32), width: 1.5))), cardTheme: CardThemeData(elevation: 0, color: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22), side: const BorderSide(color: Color(0xFFE4E9E2))))),
    home: const RootScreen(),
  );
}

class RootScreen extends StatefulWidget { const RootScreen({super.key}); @override State<RootScreen> createState() => _RootScreenState(); }
class _RootScreenState extends State<RootScreen> {
  String? role; bool loading = true; bool mustChangePassword = false;
  @override void initState() { super.initState(); _restore(); }
  Future<void> _restore() async { final p = await SharedPreferences.getInstance(); final token = p.getString('accessToken'); setState(() { role = token == null || token.isEmpty ? null : p.getString('role'); mustChangePassword = p.getBool('mustChangePassword') ?? false; loading = false; }); }
  void signedIn(String r) async { final p=await SharedPreferences.getInstance(); setState(() { role=r; mustChangePassword=p.getBool('mustChangePassword')??false; }); }
  Future<void> signOut() async { await ApiService.instance.logout(); if (mounted) setState(() => role = null); }
  @override Widget build(BuildContext context) { if (loading) return const SplashScreen(); if (role == null) return LoginScreen(onSignedIn: signedIn); if (mustChangePassword) return ChangePasswordScreen(onComplete: () => setState(() => mustChangePassword=false)); return HomeScreen(role: role!, onLogout: signOut); }
}

class SplashScreen extends StatelessWidget { const SplashScreen({super.key}); @override Widget build(BuildContext c) => const Scaffold(body: Center(child: CircularProgressIndicator())); }

class LoginScreen extends StatefulWidget { final void Function(String) onSignedIn; const LoginScreen({super.key, required this.onSignedIn}); @override State<LoginScreen> createState() => _LoginScreenState(); }
class _LoginScreenState extends State<LoginScreen> {
  final college = TextEditingController(), user = TextEditingController(), pass = TextEditingController(); bool busy = false, obscure = true;
  Future<void> submit() async {
    if (college.text.trim().isEmpty || user.text.trim().isEmpty || pass.text.isEmpty) { _error('Enter your college code, email and password.'); return; }
    setState(() => busy = true); try { final result = await ApiService.instance.login(college.text.trim(), user.text.trim(), pass.text); widget.onSignedIn((result['role'] ?? '').toString()); } catch (e) { _error(e.toString().replaceFirst('Exception: ', '')); } finally { if (mounted) setState(() => busy = false); }
  }
  void _error(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));
  @override Widget build(BuildContext context) => Scaffold(body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(24), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 480), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [Container(width: 52, height: 52, decoration: BoxDecoration(color: const Color(0xFF2E7D32), borderRadius: BorderRadius.circular(17)), child: const Center(child: Text('E', style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900))),), const SizedBox(width: 12), const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('EduSphere', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900)), Text('College ERP', style: TextStyle(color: Color(0xFF68736B)))])]),
    const SizedBox(height: 42), Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7), decoration: BoxDecoration(color: const Color(0xFFE8F5E9), borderRadius: BorderRadius.circular(999)), child: const Row(mainAxisSize: MainAxisSize.min, children: [Icon(Icons.circle, size: 8, color: Color(0xFF4CAF50)), SizedBox(width: 7), Text('MOBILE CAMPUS', style: TextStyle(color: Color(0xFF2E7D32), fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1))])),
    const SizedBox(height: 16), const Text('Your campus, beautifully connected.', style: TextStyle(fontSize: 32, height: 1.18, fontWeight: FontWeight.w900)), const SizedBox(height: 10), const Text('Classes, attendance, assignments and results in one secure mobile workspace.', style: TextStyle(color: Color(0xFF68736B), fontSize: 15, height: 1.5)), const SizedBox(height: 28), Card(child: Padding(padding: const EdgeInsets.all(22), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('Welcome back', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900)), const SizedBox(height: 4), const Text('Sign in to continue to your campus.', style: TextStyle(color: Color(0xFF68736B))), const SizedBox(height: 20), _field('College code', college, 'e.g. MREC'), _field('Email', user, 'Enter email'), TextField(controller: pass, obscureText: obscure, decoration: InputDecoration(labelText: 'Password', hintText: 'Enter password', suffixIcon: IconButton(onPressed: () => setState(() => obscure = !obscure), icon: Icon(obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined)))), const SizedBox(height: 22), SizedBox(width: double.infinity, height: 54, child: FilledButton(onPressed: busy ? null : submit, child: busy ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Sign in securely', style: TextStyle(fontWeight: FontWeight.w800))))]))), const SizedBox(height: 24), const Center(child: Text('EduSphere ERP · Secure campus access', style: TextStyle(fontSize: 11, color: Color(0xFF68736B))))
  ]))))));
  Widget _field(String label, TextEditingController c, String hint) => Padding(padding: const EdgeInsets.only(bottom: 12), child: TextField(controller: c, textCapitalization: label == 'College code' ? TextCapitalization.characters : TextCapitalization.none, decoration: InputDecoration(labelText: label, hintText: hint)));
}

class HomeScreen extends StatefulWidget { final String role; final Future<void> Function() onLogout; const HomeScreen({super.key, required this.role, required this.onLogout}); @override State<HomeScreen> createState() => _HomeScreenState(); }
class _HomeScreenState extends State<HomeScreen> {
  Map<String, dynamic>? data; bool loading = true;
  @override void initState() { super.initState(); load(); }
  Future<void> load() async { setState(() => loading = true); try { if (widget.role.toUpperCase().contains('TEACHER')) data = await ApiService.instance.teacherDashboard(); else if (widget.role.toUpperCase().contains('STUDENT')) data = await ApiService.instance.studentDashboard(); else data = {}; } catch (_) { data = {}; } finally { if (mounted) setState(() => loading = false); } }
  String get name => (data?['studentName'] ?? data?['teacherName'] ?? 'User').toString();
  bool get teacher => widget.role.toUpperCase().contains('TEACHER');
  @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('EduSphere', style: TextStyle(fontWeight: FontWeight.w900)), actions: [IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout_rounded))]), body: RefreshIndicator(onRefresh: load, child: ListView(padding: const EdgeInsets.all(20), children: [
    Text(teacher ? 'Welcome back,' : 'Good to see you,', style: const TextStyle(color: Color(0xFF68736B), fontSize: 15)), const SizedBox(height: 4), Row(children: [Expanded(child: Text(name, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900))), CircleAvatar(backgroundColor: const Color(0xFFE8F5E9), child: Text(name.isEmpty ? 'U' : name[0].toUpperCase(), style: const TextStyle(color: Color(0xFF2E7D32), fontWeight: FontWeight.w900)))]), const SizedBox(height: 22),
    if (loading) const Padding(padding: EdgeInsets.all(48), child: Center(child: CircularProgressIndicator())) else ...[
      _hero(), const SizedBox(height: 18), const Text('Quick overview', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w900)), const SizedBox(height: 12), _stats(), const SizedBox(height: 22),
      Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(teacher ? 'Teaching tools' : 'Academic tools', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)), const SizedBox(height: 12), ...['Attendance','Assignments','Timetable','Results','Profile'].map((x) => ListTile(contentPadding: EdgeInsets.zero, leading: CircleAvatar(backgroundColor: const Color(0xFFE8F5E9), child: Icon(_icon(x), color: const Color(0xFF2E7D32), size: 20)), title: Text(x, style: const TextStyle(fontWeight: FontWeight.w700)), trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 15), onTap: () => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$x is available in the ERP web module.')))))])))]
  ])));
  Widget _hero() => Container(padding: const EdgeInsets.all(22), decoration: BoxDecoration(color: const Color(0xFF2E7D32), borderRadius: BorderRadius.circular(22)), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(teacher ? 'TODAY’S TEACHING' : 'ACADEMIC PROFILE', style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.2)), const SizedBox(height: 10), Text(teacher ? 'Your teaching overview is ready.' : '${data?['course'] ?? 'Student'} · ${data?['department'] ?? 'Department'}', style: const TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900)), const SizedBox(height: 7), Text(teacher ? '${data?['upcomingClassesCount'] ?? 0} upcoming classes' : 'Semester ${data?['semester'] ?? '—'} · CGPA ${data?['cgpa'] ?? '—'}', style: const TextStyle(color: Colors.white70, fontSize: 14))]));
  Widget _stats() => Row(children: (teacher ? [['Subjects', data?['totalSubjects'] ?? 0], ['Students', data?['totalStudents'] ?? 0], ['Reviews', data?['pendingReviewCount'] ?? 0]] : [['Attendance', '${data?['attendancePercentage'] ?? 0}%'], ['Subjects', data?['totalSubjects'] ?? 0], ['Pending', data?['pendingAssignments'] ?? 0]]).map((s) => Expanded(child: Padding(padding: const EdgeInsets.only(right: 8), child: Card(child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('${s[0]}', style: const TextStyle(color: Color(0xFF68736B), fontSize: 12)), const SizedBox(height: 5), Text('${s[1]}', style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900))])))))).toList());
  IconData _icon(String x) => switch (x) { 'Attendance' => Icons.fact_check_outlined, 'Assignments' => Icons.assignment_outlined, 'Timetable' => Icons.calendar_month_outlined, 'Results' => Icons.bar_chart_rounded, _ => Icons.person_outline_rounded };
}
