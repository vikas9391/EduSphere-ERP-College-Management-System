import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

part 'auth_screens.dart';
part 'assignments.dart';
part 'erp_ui.dart';

const apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://localhost:8080/api');
const navy = Color(0xFF2E7D32);
const indigo = Color(0xFF256428);
const pageBg = Color(0xFFF8F8F2);
const muted = Color(0xFF6B7280);
const green = Color(0xFF4CAF50);
const lightGreen = Color(0xFFE8F5E9);
const border = Color(0xFFEEF2E7);
const textColor = Color(0xFF1F2937);

String cleanError(Object error) => error.toString().replaceFirst('Exception: ', '');
void snack(BuildContext context, String message) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));

class ApiService {
  ApiService._();
  static final instance = ApiService._();
  String get base => apiUrl.replaceFirst(RegExp(r'/$'), '');

  Future<dynamic> request(String path, {String method = 'GET', Map<String, dynamic>? body, bool retry = true}) async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken');
    final headers = <String, String>{
      'Accept': 'application/json',
      if (body != null) 'Content-Type': 'application/json',
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer ' + token,
    };
    final uri = Uri.parse(base + path);
    http.Response response;
    if (method == 'POST') {
      response = await http.post(uri, headers: headers, body: body == null ? null : jsonEncode(body));
    } else if (method == 'PUT') {
      response = await http.put(uri, headers: headers, body: body == null ? null : jsonEncode(body));
    } else if (method == 'DELETE') {
      response = await http.delete(uri, headers: headers);
    } else {
      response = await http.get(uri, headers: headers);
    }
    if (response.statusCode == 401 && retry && !path.startsWith('/auth/')) {
      if (await refresh()) return request(path, method: method, body: body, retry: false);
    }
    dynamic data;
    try {
      data = response.body.isEmpty ? null : jsonDecode(response.body);
    } catch (_) {
      data = null;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception(data is Map ? (data['message'] ?? data['error'] ?? 'Request failed (' + response.statusCode.toString() + ')') : 'Request failed (' + response.statusCode.toString() + ')');
    }
    if (data is Map && data['data'] != null) return data['data'];
    return data;
  }

  Future<bool> refresh() async {
    final prefs = await SharedPreferences.getInstance();
    final refreshToken = prefs.getString('refreshToken');
    if (refreshToken == null || refreshToken.isEmpty) return false;
    try {
      final r = await http.post(
        Uri.parse(base + '/auth/refresh'),
        headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );
      if (r.statusCode < 200 || r.statusCode >= 300) throw Exception();
      final raw = jsonDecode(r.body);
      final data = raw is Map && raw['data'] is Map ? raw['data'] : raw;
      if (data is! Map || data['accessToken'] == null) throw Exception();
      await prefs.setString('accessToken', data['accessToken'].toString());
      if (data['refreshToken'] != null) await prefs.setString('refreshToken', data['refreshToken'].toString());
      if (data['role'] != null) await prefs.setString('role', data['role'].toString());
      return true;
    } catch (_) {
      await logout();
      return false;
    }
  }

  Future<Map<String, dynamic>> login(String collegeCode, String email, String password) async {
    final data = await request('/auth/login', method: 'POST', retry: false, body: {
      'collegeCode': collegeCode,
      'email': email,
      'password': password,
    });
    final value = data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{};
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('accessToken', (value['accessToken'] ?? '').toString());
    await prefs.setString('email', (value['email'] ?? email).toString());
    await prefs.setString('role', (value['role'] ?? '').toString());
    await prefs.setBool('mustChangePassword', value['mustChangePassword'] == true);
    if (value['refreshToken'] != null) await prefs.setString('refreshToken', value['refreshToken'].toString());
    return value;
  }

  Future<void> logout() async {
    final p = await SharedPreferences.getInstance();
    await p.remove('accessToken');
    await p.remove('refreshToken');
    await p.remove('role');
    await p.remove('email');
    await p.remove('mustChangePassword');
  }

  Future<Map<String, dynamic>> studentDashboard() async {
    final v = await request('/student/dashboard');
    return v is Map ? Map<String, dynamic>.from(v) : {};
  }

  Future<Map<String, dynamic>> teacherDashboard() async {
    final v = await request('/teacher/dashboard');
    return v is Map ? Map<String, dynamic>.from(v) : {};
  }

  Future<List<Map<String, dynamic>>> list(String path) async {
    final v = await request(path);
    if (v is! List) return [];
    return v.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
  }

  Future<Map<String, dynamic>> mapPost(String path, Map<String, dynamic> body) async {
    final v = await request(path, method: 'POST', body: body);
    return v is Map ? Map<String, dynamic>.from(v) : {};
  }

  Future<Map<String, dynamic>> mapPut(String path, Map<String, dynamic> body) async {
    final v = await request(path, method: 'PUT', body: body);
    return v is Map ? Map<String, dynamic>.from(v) : {};
  }

  Future<List<Map<String, dynamic>>> studentAssignments() => list('/student/assignments');
  Future<List<Map<String, dynamic>>> teacherAssignments() => list('/teacher/assignments');
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
      scaffoldBackgroundColor: pageBg,
      colorScheme: ColorScheme.fromSeed(seedColor: navy, brightness: Brightness.light),
      fontFamily: 'Roboto',
      appBarTheme: const AppBarTheme(backgroundColor: pageBg, foregroundColor: textColor, elevation: 0),
      snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating, backgroundColor: textColor),
      cardTheme: CardThemeData(
        elevation: 0,
        color: Colors.white,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
          side: const BorderSide(color: border),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: const BorderSide(color: border)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: const BorderSide(color: border)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: const BorderSide(color: navy, width: 1.5)),
      ),
    ),
    home: const RootScreen(),
  );
}

class RootScreen extends StatefulWidget {
  const RootScreen({super.key});
  @override State<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends State<RootScreen> {
  String? role;
  bool loading = true;
  bool mustChangePassword = false;

  @override
  void initState() {
    super.initState();
    restore();
  }

  Future<void> restore() async {
    final p = await SharedPreferences.getInstance();
    final token = p.getString('accessToken');
    if (!mounted) return;
    setState(() {
      role = token == null || token.isEmpty ? null : p.getString('role');
      mustChangePassword = p.getBool('mustChangePassword') ?? false;
      loading = false;
    });
  }

  Future<void> signedIn(String value) async {
    final p = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      role = value;
      mustChangePassword = p.getBool('mustChangePassword') ?? false;
    });
  }

  Future<void> signOut() async {
    await ApiService.instance.logout();
    if (mounted) setState(() => role = null);
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const SplashScreen();
    if (role == null) return LoginScreen(onSignedIn: signedIn);
    if (mustChangePassword) return ChangePasswordScreen(onComplete: () => setState(() => mustChangePassword = false));
    return ErpShell(role: role!, onLogout: signOut);
  }
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});
  @override
  Widget build(BuildContext context) => const BookLoadingScreen(label: 'Preparing your campus…');
}

class LoginScreen extends StatefulWidget {
  final Future<void> Function(String) onSignedIn;
  const LoginScreen({super.key, required this.onSignedIn});
  @override State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final college = TextEditingController();
  final email = TextEditingController();
  final password = TextEditingController();
  bool busy = false;
  bool obscure = true;

  @override
  void dispose() {
    college.dispose();
    email.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (college.text.trim().isEmpty || email.text.trim().isEmpty || password.text.isEmpty) {
      snack(context, 'Enter your college code, email and password.');
      return;
    }
    setState(() => busy = true);
    try {
      final result = await ApiService.instance.login(college.text.trim(), email.text.trim(), password.text);
      await widget.onSignedIn((result['role'] ?? '').toString());
    } catch (e) {
      snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(28, 38, 28, 28),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Column(
              children: [
                Container(
                  width: 76, height: 76,
                  decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(24)),
                  child: const Center(child: Icon(Icons.spa_rounded, color: navy, size: 40)),
                ),
                const SizedBox(height: 18),
                const Text('EduSphere', style: TextStyle(fontSize: 34, fontWeight: FontWeight.w900)),
                const SizedBox(height: 4),
                const Text('Smart Campus, Smarter Future', style: TextStyle(color: muted, fontSize: 14, fontWeight: FontWeight.w600)),
                const SizedBox(height: 34),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Text('Welcome back', style: TextStyle(fontSize: 27, fontWeight: FontWeight.w900)),
                      const SizedBox(height: 5),
                      const Text('Sign in to continue to your campus', style: TextStyle(color: muted, fontSize: 15)),
                      const SizedBox(height: 24),
                      TextField(controller: college, textCapitalization: TextCapitalization.characters, decoration: const InputDecoration(labelText: 'College code', hintText: 'e.g. MREC', prefixIcon: Icon(Icons.account_balance_outlined))),
                      const SizedBox(height: 13),
                      TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email address', hintText: 'you@example.com', prefixIcon: Icon(Icons.mail_outline_rounded))),
                      const SizedBox(height: 13),
                      TextField(controller: password, obscureText: obscure, decoration: InputDecoration(labelText: 'Password', prefixIcon: const Icon(Icons.lock_outline_rounded), suffixIcon: IconButton(onPressed: () => setState(() => obscure = !obscure), icon: Icon(obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined)))),
                      const SizedBox(height: 22),
                      SizedBox(
                        width: double.infinity, height: 54,
                        child: FilledButton.icon(
                          onPressed: busy ? null : submit,
                          icon: const Icon(Icons.bolt_rounded),
                          label: Text(busy ? 'Signing in...' : 'Sign In', style: const TextStyle(fontWeight: FontWeight.w800)),
                        ),
                      ),
                      const SizedBox(height: 10),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ForgotPasswordScreen())),
                          child: const Text('Forgot password?'),
                        ),
                      ),
                    ]),
                  ),
                ),
                const SizedBox(height: 18),
                const Text('EduSphere ERP · Smart Campus, Smarter Future', style: TextStyle(color: muted, fontSize: 12)),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}
