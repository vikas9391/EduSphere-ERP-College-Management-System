import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

const apiUrl = String.fromEnvironment(
  'API_URL',
  defaultValue: 'http://localhost:8080/api',
);

class ApiService {
  ApiService._();
  static final instance = ApiService._();

  String get base => apiUrl.replaceFirst(RegExp(r'/$'), '');

  Future<dynamic> _rawRequest(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
    bool retry = true,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken');
    final headers = <String, String>{
      'Accept': 'application/json',
      if (body != null) 'Content-Type': 'application/json',
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };
    final uri = Uri.parse('$base$path');
    late http.Response response;

    switch (method) {
      case 'POST':
        response = await http.post(
          uri,
          headers: headers,
          body: body == null ? null : jsonEncode(body),
        );
        break;
      case 'PUT':
        response = await http.put(
          uri,
          headers: headers,
          body: body == null ? null : jsonEncode(body),
        );
        break;
      case 'DELETE':
        response = await http.delete(uri, headers: headers);
        break;
      default:
        response = await http.get(uri, headers: headers);
    }

    if (response.statusCode == 401 && retry && !path.startsWith('/auth/')) {
      if (await refresh()) {
        return _rawRequest(path, method: method, body: body, retry: false);
      }
    }

    dynamic data;
    try {
      data = response.body.isEmpty ? null : jsonDecode(response.body);
    } catch (_) {
      data = null;
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = data is Map
          ? (data['message'] ?? data['error'] ?? 'Request failed (${response.statusCode})')
          : 'Request failed (${response.statusCode})';
      throw Exception(message.toString());
    }

    if (data is Map && data['data'] != null) return data['data'];
    return data;
  }

  Future<Map<String, dynamic>> _mapRequest(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
    bool retry = true,
  }) async {
    final data = await _rawRequest(
      path,
      method: method,
      body: body,
      retry: retry,
    );
    return data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{};
  }

  Future<List<dynamic>> _listRequest(String path) async {
    final data = await _rawRequest(path);
    return data is List ? data : <dynamic>[];
  }

  Future<bool> refresh() async {
    final prefs = await SharedPreferences.getInstance();
    final refreshToken = prefs.getString('refreshToken');
    if (refreshToken == null || refreshToken.isEmpty) return false;

    try {
      final response = await http.post(
        Uri.parse('$base/auth/refresh'),
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'refreshToken': refreshToken}),
      );
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw Exception();
      }
      final raw = jsonDecode(response.body);
      final data = raw is Map && raw['data'] is Map ? raw['data'] : raw;
      if (data is! Map || data['accessToken'] == null) throw Exception();
      await prefs.setString('accessToken', data['accessToken'].toString());
      if (data['refreshToken'] != null) {
        await prefs.setString('refreshToken', data['refreshToken'].toString());
      }
      if (data['role'] != null) await prefs.setString('role', data['role'].toString());
      return true;
    } catch (_) {
      await logout();
      return false;
    }
  }

  Future<Map<String, dynamic>> login(
    String collegeCode,
    String username,
    String password,
  ) async {
    final data = await _mapRequest(
      '/auth/login',
      method: 'POST',
      retry: false,
      body: {
        'collegeCode': collegeCode,
        'username': username,
        'password': password,
      },
    );
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('accessToken', (data['accessToken'] ?? '').toString());
    await prefs.setString('username', (data['username'] ?? username).toString());
    await prefs.setString('role', (data['role'] ?? '').toString());
    if (data['refreshToken'] != null) {
      await prefs.setString('refreshToken', data['refreshToken'].toString());
    }
    return data;
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('accessToken');
    await prefs.remove('refreshToken');
    await prefs.remove('role');
    await prefs.remove('username');
  }

  Future<Map<String, dynamic>> studentDashboard() => _mapRequest('/student/dashboard');
  Future<Map<String, dynamic>> teacherDashboard() => _mapRequest('/teacher/dashboard');

  Future<List<Map<String, dynamic>>> teacherStudents() async =>
      (await _listRequest('/teacher/students'))
          .whereType<Map>()
          .map((e) => Map<String, dynamic>.from(e))
          .toList();

  Future<List<Map<String, dynamic>>> myAttendance() async =>
      (await _listRequest('/attendance'))
          .whereType<Map>()
          .map((e) => Map<String, dynamic>.from(e))
          .toList();

  Future<Map<String, dynamic>> createAttendance(Map<String, dynamic> body) =>
      _mapRequest('/attendance', method: 'POST', body: body);

  Future<Map<String, dynamic>> updateAttendance(
    int id,
    Map<String, dynamic> body,
  ) => _mapRequest('/attendance/$id', method: 'PUT', body: body);

  Future<Map<String, dynamic>> studentAttendanceSummary() =>
      _mapRequest('/attendance/me/summary');
}

void main() => runApp(const EduSphereApp());

class EduSphereApp extends StatelessWidget {
  const EduSphereApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'EduSphere ERP',
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF2E7D32),
            brightness: Brightness.light,
          ),
          scaffoldBackgroundColor: const Color(0xFFF6F7F2),
          inputDecorationTheme: InputDecorationTheme(
            filled: true,
            fillColor: const Color(0xFFF0F4EE),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: Color(0xFFE4E9E2)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: Color(0xFF2E7D32), width: 1.5),
            ),
          ),
          cardTheme: CardThemeData(
            elevation: 0,
            color: Colors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(22),
              side: const BorderSide(color: Color(0xFFE4E9E2)),
            ),
          ),
        ),
        home: const RootScreen(),
      );
}

class RootScreen extends StatefulWidget {
  const RootScreen({super.key});
  @override
  State<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends State<RootScreen> {
  String? role;
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken');
    if (!mounted) return;
    setState(() {
      role = token == null || token.isEmpty ? null : prefs.getString('role');
      loading = false;
    });
  }

  void signedIn(String value) => setState(() => role = value);

  Future<void> signOut() async {
    await ApiService.instance.logout();
    if (mounted) setState(() => role = null);
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const SplashScreen();
    if (role == null) return LoginScreen(onSignedIn: signedIn);
    return HomeScreen(role: role!, onLogout: signOut);
  }
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});
  @override
  Widget build(BuildContext context) => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
}

class LoginScreen extends StatefulWidget {
  final void Function(String) onSignedIn;
  const LoginScreen({super.key, required this.onSignedIn});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final college = TextEditingController();
  final user = TextEditingController();
  final pass = TextEditingController();
  bool busy = false;
  bool obscure = true;

  Future<void> submit() async {
    if (college.text.trim().isEmpty || user.text.trim().isEmpty || pass.text.isEmpty) {
      _error('Enter your college code, username and password.');
      return;
    }
    setState(() => busy = true);
    try {
      final result = await ApiService.instance.login(
        college.text.trim(),
        user.text.trim(),
        pass.text,
      );
      widget.onSignedIn((result['role'] ?? '').toString());
    } catch (e) {
      _error(e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  void _error(String message) => ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      Container(
                        width: 52,
                        height: 52,
                        decoration: BoxDecoration(
                          color: const Color(0xFF2E7D32),
                          borderRadius: BorderRadius.circular(17),
                        ),
                        child: const Center(
                          child: Text(
                            'E',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 28,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('EduSphere', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
                          Text('College ERP', style: TextStyle(color: Color(0xFF68736B))),
                        ],
                      ),
                    ]),
                    const SizedBox(height: 42),
                    const Text(
                      'Your campus, beautifully connected.',
                      style: TextStyle(fontSize: 32, height: 1.18, fontWeight: FontWeight.w900),
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      'Classes, attendance, assignments and results in one secure mobile workspace.',
                      style: TextStyle(color: Color(0xFF68736B), fontSize: 15, height: 1.5),
                    ),
                    const SizedBox(height: 28),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(22),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Welcome back', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
                            const SizedBox(height: 4),
                            const Text('Sign in to continue to your campus.', style: TextStyle(color: Color(0xFF68736B))),
                            const SizedBox(height: 20),
                            _field('College code', college, 'e.g. MREC'),
                            _field('Username', user, 'Enter username'),
                            TextField(
                              controller: pass,
                              obscureText: obscure,
                              decoration: InputDecoration(
                                labelText: 'Password',
                                hintText: 'Enter password',
                                suffixIcon: IconButton(
                                  onPressed: () => setState(() => obscure = !obscure),
                                  icon: Icon(obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                                ),
                              ),
                            ),
                            const SizedBox(height: 22),
                            SizedBox(
                              width: double.infinity,
                              height: 54,
                              child: FilledButton(
                                onPressed: busy ? null : submit,
                                child: busy
                                    ? const SizedBox(
                                        width: 22,
                                        height: 22,
                                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                      )
                                    : const Text('Sign in securely', style: TextStyle(fontWeight: FontWeight.w800)),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );

  Widget _field(String label, TextEditingController controller, String hint) => Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: TextField(
          controller: controller,
          textCapitalization: label == 'College code' ? TextCapitalization.characters : TextCapitalization.none,
          decoration: InputDecoration(labelText: label, hintText: hint),
        ),
      );
}

class HomeScreen extends StatefulWidget {
  final String role;
  final Future<void> Function() onLogout;
  const HomeScreen({super.key, required this.role, required this.onLogout});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  Map<String, dynamic>? data;
  bool loading = true;

  bool get teacher => widget.role.toUpperCase().contains('TEACHER');
  bool get student => widget.role.toUpperCase().contains('STUDENT');

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() => loading = true);
    try {
      if (teacher) {
        data = await ApiService.instance.teacherDashboard();
      } else if (student) {
        data = await ApiService.instance.studentDashboard();
      } else {
        data = {};
      }
    } catch (_) {
      data = {};
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  String get name => (data?['studentName'] ?? data?['teacherName'] ?? 'User').toString();

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('EduSphere', style: TextStyle(fontWeight: FontWeight.w900)),
          actions: [IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout_rounded))],
        ),
        body: RefreshIndicator(
          onRefresh: load,
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Text(teacher ? 'Welcome back,' : 'Good to see you,', style: const TextStyle(color: Color(0xFF68736B), fontSize: 15)),
              const SizedBox(height: 4),
              Row(children: [
                Expanded(child: Text(name, style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900))),
                CircleAvatar(
                  backgroundColor: const Color(0xFFE8F5E9),
                  child: Text(name.isEmpty ? 'U' : name[0].toUpperCase(), style: const TextStyle(color: Color(0xFF2E7D32), fontWeight: FontWeight.w900)),
                ),
              ]),
              const SizedBox(height: 22),
              if (loading)
                const Padding(padding: EdgeInsets.all(48), child: Center(child: CircularProgressIndicator()))
              else ...[
                _hero(),
                const SizedBox(height: 18),
                const Text('Quick overview', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
                const SizedBox(height: 12),
                _stats(),
                const SizedBox(height: 22),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(teacher ? 'Teaching tools' : 'Academic tools', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
                        const SizedBox(height: 12),
                        _tool('Classes', Icons.school_outlined, () => Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherClassesScreen() : const StudentClassesScreen()))),
                        _tool('Attendance', Icons.fact_check_outlined, () => Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherAttendanceScreen() : const StudentAttendanceScreen()))),
                        _tool('Assignments', Icons.assignment_outlined, () => _comingSoon('Assignments')),
                        _tool('Timetable', Icons.calendar_month_outlined, () => _comingSoon('Timetable')),
                        _tool('Results', Icons.bar_chart_rounded, () => _comingSoon('Results')),
                        _tool('Profile', Icons.person_outline_rounded, () => _comingSoon('Profile')),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      );

  Widget _tool(String title, IconData icon, VoidCallback onTap) => ListTile(
        contentPadding: EdgeInsets.zero,
        leading: CircleAvatar(
          backgroundColor: const Color(0xFFE8F5E9),
          child: Icon(icon, color: const Color(0xFF2E7D32), size: 20),
        ),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
        trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 15),
        onTap: onTap,
      );

  void _comingSoon(String title) => ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$title mobile module is next in the Flutter build.'), behavior: SnackBarBehavior.floating),
      );

  Widget _hero() => Container(
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(color: const Color(0xFF2E7D32), borderRadius: BorderRadius.circular(22)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(teacher ? 'TODAY’S TEACHING' : 'ACADEMIC PROFILE', style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.2)),
            const SizedBox(height: 10),
            Text(
              teacher ? 'Your teaching overview is ready.' : '${data?['course'] ?? 'Student'} · ${data?['department'] ?? 'Department'}',
              style: const TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 7),
            Text(
              teacher ? '${data?['upcomingClassesCount'] ?? 0} upcoming classes' : 'Semester ${data?['semester'] ?? '—'} · CGPA ${data?['cgpa'] ?? '—'}',
              style: const TextStyle(color: Colors.white70, fontSize: 14),
            ),
          ],
        ),
      );

  Widget _stats() => Row(
        children: (teacher
                ? [['Subjects', data?['totalSubjects'] ?? 0], ['Students', data?['totalStudents'] ?? 0], ['Reviews', data?['pendingReviewCount'] ?? 0]]
                : [['Attendance', '${data?['attendancePercentage'] ?? 0}%'], ['Subjects', data?['totalSubjects'] ?? 0], ['Pending', data?['pendingAssignments'] ?? 0]])
            .map((item) => Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text('${item[0]}', style: const TextStyle(color: Color(0xFF68736B), fontSize: 12)),
                          const SizedBox(height: 5),
                          Text('${item[1]}', style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900)),
                        ]),
                      ),
                    ),
                  ),
                ))
            .toList(),
      );
}

class StudentClassesScreen extends StatelessWidget {
  const StudentClassesScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('My Classes', style: TextStyle(fontWeight: FontWeight.w900))),
        body: FutureBuilder<Map<String, dynamic>>(
          future: ApiService.instance.studentDashboard(),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) return const Center(child: CircularProgressIndicator());
            if (snapshot.hasError) return _ErrorView(message: snapshot.error.toString());
            final data = snapshot.data ?? {};
            final subjects = (data['subjects'] is List) ? List<dynamic>.from(data['subjects']) : <dynamic>[];
            return ListView(
              padding: const EdgeInsets.all(20),
              children: [
                const Text('Enrolled subjects', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
                const SizedBox(height: 8),
                Text('${subjects.length} subject${subjects.length == 1 ? '' : 's'}', style: const TextStyle(color: Color(0xFF68736B))),
                const SizedBox(height: 16),
                if (subjects.isEmpty) const _EmptyCard(title: 'No classes yet', message: 'Your enrolled subjects will appear here.'),
                ...subjects.map((item) => Card(
                      child: ListTile(
                        leading: const CircleAvatar(backgroundColor: Color(0xFFE8F5E9), child: Icon(Icons.menu_book_outlined, color: Color(0xFF2E7D32))),
                        title: Text((item is Map ? (item['subjectName'] ?? item['name'] ?? 'Subject') : item).toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
                        subtitle: Text(item is Map ? (item['subjectCode'] ?? item['code'] ?? '')?.toString() ?? '' : ''),
                      ),
                    )),
              ],
            );
          },
        ),
      );
}

class TeacherClassesScreen extends StatefulWidget {
  const TeacherClassesScreen({super.key});

  @override
  State<TeacherClassesScreen> createState() => _TeacherClassesScreenState();
}

class _TeacherClassesScreenState extends State<TeacherClassesScreen> {
  bool loading = true;
  String? error;
  List<Map<String, dynamic>> rows = [];

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      rows = await ApiService.instance.teacherStudents();
    } catch (e) {
      error = e.toString().replaceFirst('Exception: ', '');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final grouped = <String, List<Map<String, dynamic>>>{};
    for (final row in rows) {
      final key = '${row['classSubjectId'] ?? ''}';
      grouped.putIfAbsent(key, () => []).add(row);
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Classes', style: TextStyle(fontWeight: FontWeight.w900)),
        actions: [IconButton(onPressed: load, icon: const Icon(Icons.refresh_rounded))],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
              ? _ErrorView(message: error!)
              : RefreshIndicator(
                  onRefresh: load,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      const Text('Assigned teaching groups', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
                      const SizedBox(height: 6),
                      Text('${grouped.length} class subject${grouped.length == 1 ? '' : 's'} · ${rows.length} students', style: const TextStyle(color: Color(0xFF68736B))),
                      const SizedBox(height: 16),
                      if (grouped.isEmpty) const _EmptyCard(title: 'No assigned classes', message: 'Classes assigned to you will appear here.'),
                      ...grouped.entries.map((entry) {
                        final first = entry.value.first;
                        return Card(
                          child: ExpansionTile(
                            title: Text((first['className'] ?? 'Class').toString(), style: const TextStyle(fontWeight: FontWeight.w900)),
                            subtitle: Text('${first['subjectName'] ?? 'Subject'} · ${entry.value.length} students'),
                            children: entry.value.map((student) => ListTile(
                                  leading: CircleAvatar(
                                    backgroundColor: const Color(0xFFE8F5E9),
                                    child: Text(_initial(student['studentName'])),
                                  ),
                                  title: Text((student['studentName'] ?? 'Student').toString(), style: const TextStyle(fontWeight: FontWeight.w700)),
                                  subtitle: Text((student['admissionNo'] ?? '').toString()),
                                )).toList(),
                          ),
                        );
                      }),
                    ],
                  ),
                ),
    );
  }

  String _initial(dynamic value) {
    final text = value?.toString().trim() ?? '';
    return text.isEmpty ? 'S' : text[0].toUpperCase();
  }
}

class TeacherAttendanceScreen extends StatefulWidget {
  const TeacherAttendanceScreen({super.key});

  @override
  State<TeacherAttendanceScreen> createState() => _TeacherAttendanceScreenState();
}

class _TeacherAttendanceScreenState extends State<TeacherAttendanceScreen> {
  bool loading = true;
  bool saving = false;
  String? error;
  List<Map<String, dynamic>> students = [];
  List<Map<String, dynamic>> attendance = [];
  String? selectedGroup;
  DateTime selectedDate = DateTime.now();
  final statuses = <String, String>{};

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final results = await Future.wait([
        ApiService.instance.teacherStudents(),
        ApiService.instance.myAttendance(),
      ]);
      students = results[0] as List<Map<String, dynamic>>;
      attendance = results[1] as List<Map<String, dynamic>>;
      final groups = _groups;
      if (selectedGroup == null || !groups.containsKey(selectedGroup)) {
        selectedGroup = groups.keys.isEmpty ? null : groups.keys.first;
      }
      _loadStatuses();
    } catch (e) {
      error = e.toString().replaceFirst('Exception: ', '');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Map<String, List<Map<String, dynamic>>> get _groups {
    final result = <String, List<Map<String, dynamic>>>{};
    for (final student in students) {
      final key = '${student['classSubjectId'] ?? ''}';
      result.putIfAbsent(key, () => []).add(student);
    }
    return result;
  }

  List<Map<String, dynamic>> get _selectedStudents =>
      selectedGroup == null ? [] : (_groups[selectedGroup] ?? []);

  String get _date => '${selectedDate.year.toString().padLeft(4, '0')}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}';

  void _loadStatuses() {
    statuses.clear();
    for (final student in _selectedStudents) {
      final enrollmentId = '${student['id']}';
      final record = attendance.cast<Map<String, dynamic>?>().firstWhere(
        (item) => '${item?['classEnrollmentId']}' == enrollmentId && '${item?['attendanceDate']}' == _date,
        orElse: () => null,
      );
      statuses[enrollmentId] = record?['status']?.toString().toUpperCase() ?? 'PRESENT';
    }
  }

  Future<void> _pickDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (date != null) setState(() { selectedDate = date; _loadStatuses(); });
  }

  Future<void> _save() async {
    if (_selectedStudents.isEmpty) return;
    setState(() => saving = true);
    try {
      for (final student in _selectedStudents) {
        final enrollmentId = int.tryParse('${student['id']}');
        if (enrollmentId == null) continue;
        final payload = {
          'classEnrollmentId': enrollmentId,
          'attendanceDate': _date,
          'status': statuses['$enrollmentId'] ?? 'PRESENT',
          'remarks': '',
        };
        final existing = attendance.cast<Map<String, dynamic>?>().firstWhere(
          (item) => '${item?['classEnrollmentId']}' == '$enrollmentId' && '${item?['attendanceDate']}' == _date,
          orElse: () => null,
        );
        if (existing?['id'] != null) {
          await ApiService.instance.updateAttendance(int.parse('${existing!['id']}'), payload);
        } else {
          await ApiService.instance.createAttendance(payload);
        }
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Attendance saved successfully'), behavior: SnackBarBehavior.floating));
      }
      await load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))));
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final groups = _groups;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mark Attendance', style: TextStyle(fontWeight: FontWeight.w900)),
        actions: [IconButton(onPressed: loading ? null : _pickDate, icon: const Icon(Icons.event_outlined))],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
              ? _ErrorView(message: error!)
              : Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 18, 20, 8),
                      child: Column(
                        children: [
                          DropdownButtonFormField<String>(
                            value: selectedGroup,
                            decoration: const InputDecoration(labelText: 'Class / subject'),
                            items: groups.entries.map((entry) {
                              final first = entry.value.first;
                              return DropdownMenuItem(
                                value: entry.key,
                                child: Text('${first['className'] ?? 'Class'} · ${first['subjectName'] ?? 'Subject'}'),
                              );
                            }).toList(),
                            onChanged: (value) => setState(() { selectedGroup = value; _loadStatuses(); }),
                          ),
                          const SizedBox(height: 12),
                          InkWell(
                            onTap: _pickDate,
                            borderRadius: BorderRadius.circular(16),
                            child: InputDecorator(
                              decoration: const InputDecoration(labelText: 'Attendance date'),
                              child: Text(_date, style: const TextStyle(fontWeight: FontWeight.w700)),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: _selectedStudents.isEmpty
                          ? const _EmptyCard(title: 'No students', message: 'No class roster is assigned to this teaching group.')
                          : ListView.builder(
                              padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
                              itemCount: _selectedStudents.length,
                              itemBuilder: (context, index) {
                                final student = _selectedStudents[index];
                                final id = '${student['id']}';
                                final status = statuses[id] ?? 'PRESENT';
                                return Card(
                                  child: Padding(
                                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                    child: Row(
                                      children: [
                                        CircleAvatar(
                                          backgroundColor: const Color(0xFFE8F5E9),
                                          child: Text(_initial(student['studentName'])),
                                        ),
                                        const SizedBox(width: 12),
                                        Expanded(
                                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                            Text((student['studentName'] ?? 'Student').toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
                                            Text((student['admissionNo'] ?? '').toString(), style: const TextStyle(color: Color(0xFF68736B), fontSize: 12)),
                                          ]),
                                        ),
                                        PopupMenuButton<String>(
                                          initialValue: status,
                                          onSelected: (value) => setState(() => statuses[id] = value),
                                          itemBuilder: (_) => const [
                                            PopupMenuItem(value: 'PRESENT', child: Text('Present')),
                                            PopupMenuItem(value: 'ABSENT', child: Text('Absent')),
                                            PopupMenuItem(value: 'LATE', child: Text('Late')),
                                            PopupMenuItem(value: 'EXCUSED', child: Text('Excused')),
                                            PopupMenuItem(value: 'HOLIDAY', child: Text('Holiday')),
                                          ],
                                          child: Chip(label: Text(status)),
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                    ),
                  ],
                ),
      floatingActionButton: (!loading && error == null && _selectedStudents.isNotEmpty)
          ? FloatingActionButton.extended(
              onPressed: saving ? null : _save,
              icon: saving ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.save_outlined),
              label: Text(saving ? 'Saving…' : 'Save attendance'),
            )
          : null,
    );
  }

  String _initial(dynamic value) {
    final text = value?.toString().trim() ?? '';
    return text.isEmpty ? 'S' : text[0].toUpperCase();
  }
}

class StudentAttendanceScreen extends StatelessWidget {
  const StudentAttendanceScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Attendance', style: TextStyle(fontWeight: FontWeight.w900))),
        body: FutureBuilder<Map<String, dynamic>>(
          future: ApiService.instance.studentAttendanceSummary(),
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) return const Center(child: CircularProgressIndicator());
            if (snapshot.hasError) return _ErrorView(message: snapshot.error.toString());
            final data = snapshot.data ?? {};
            final percentage = data['overallAttendancePercentage'] ?? data['attendancePercentage'] ?? 0;
            final bySubject = data['bySubject'] is List ? List<dynamic>.from(data['bySubject']) : <dynamic>[];
            return ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Container(
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(color: const Color(0xFF2E7D32), borderRadius: BorderRadius.circular(24)),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('OVERALL ATTENDANCE', style: TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.2)),
                    const SizedBox(height: 8),
                    Text('${_number(percentage)}%', style: const TextStyle(color: Colors.white, fontSize: 44, fontWeight: FontWeight.w900)),
                    Text('${data['classesAttended'] ?? 0} attended · ${data['classesMissed'] ?? 0} missed', style: const TextStyle(color: Colors.white70)),
                  ]),
                ),
                const SizedBox(height: 22),
                const Text('By subject', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
                const SizedBox(height: 10),
                if (bySubject.isEmpty) const _EmptyCard(title: 'No attendance records', message: 'Attendance details will appear here.'),
                ...bySubject.map((item) {
                  final map = item is Map ? item : <String, dynamic>{};
                  return Card(
                    child: ListTile(
                      title: Text((map['subjectName'] ?? 'Subject').toString(), style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text('${map['classesAttended'] ?? 0} attended · ${map['classesMissed'] ?? 0} missed'),
                      trailing: Text('${_number(map['attendancePercentage'] ?? 0)}%', style: const TextStyle(fontWeight: FontWeight.w900, color: Color(0xFF2E7D32))),
                    ),
                  );
                }),
              ],
            );
          },
        ),
      );

  static String _number(dynamic value) {
    final number = double.tryParse('$value') ?? 0;
    return number % 1 == 0 ? number.toStringAsFixed(0) : number.toStringAsFixed(1);
  }
}

class _EmptyCard extends StatelessWidget {
  final String title;
  final String message;
  const _EmptyCard({required this.title, required this.message});

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(22),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
            const SizedBox(height: 6),
            Text(message, style: const TextStyle(color: Color(0xFF68736B), height: 1.4)),
          ]),
        ),
      );
}

class _ErrorView extends StatelessWidget {
  final String message;
  const _ErrorView({required this.message});

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.cloud_off_rounded, size: 48, color: Color(0xFF68736B)),
            const SizedBox(height: 12),
            const Text('Unable to load data', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
            const SizedBox(height: 6),
            Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Color(0xFF68736B))),
          ]),
        ),
      );
}
