part of 'main.dart';

class ErpShell extends StatefulWidget {
  final String role;
  final Future<void> Function() onLogout;
  const ErpShell({super.key, required this.role, required this.onLogout});
  @override State<ErpShell> createState() => _ErpShellState();
}

class _ErpShellState extends State<ErpShell> {
  int index = 0;

  String get roleLabel {
    final r = widget.role.toUpperCase();
    if (r.contains('TEACHER')) return 'Teacher';
    if (r.contains('ADMIN')) return 'Administrator';
    return 'Student';
  }

  void select(int value) => setState(() => index = value);

  @override
  Widget build(BuildContext context) {
    final pages = [
      ErpHome(role: widget.role, onNavigate: select),
      ErpAcademics(role: widget.role),
      ErpAnalytics(role: widget.role),
      const ErpCommunity(),
      ErpProfile(role: widget.role, onLogout: widget.onLogout),
    ];
    return Scaffold(
      body: IndexedStack(index: index, children: pages),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(14, 0, 14, 12),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(32),
            border: Border.all(color: const Color(0xFFE6E1D9)),
            boxShadow: const [BoxShadow(color: Color(0x14000000), blurRadius: 24, offset: Offset(0, 8))],
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _nav(0, Icons.home_rounded, 'Home'),
              _nav(1, Icons.swap_horiz_rounded, 'Academics'),
              _nav(2, Icons.bar_chart_rounded, 'Analytics'),
              _nav(3, Icons.groups_rounded, 'Community'),
              _nav(4, Icons.person_outline_rounded, 'Profile'),
            ],
          ),
        ),
      ),
    );
  }

  Widget _nav(int value, IconData icon, String label) {
    final active = index == value;
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: () => select(value),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 7),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icon, size: 23, color: active ? navy : const Color(0xFF77736D)),
            const SizedBox(height: 3),
            Text(label, style: TextStyle(fontSize: 11, fontWeight: active ? FontWeight.w800 : FontWeight.w500, color: active ? navy : muted)),
            const SizedBox(height: 3),
            AnimatedContainer(duration: const Duration(milliseconds: 180), width: active ? 5 : 0, height: 5, decoration: const BoxDecoration(color: navy, shape: BoxShape.circle)),
          ]),
        ),
      ),
    );
  }
}

class ErpHome extends StatefulWidget {
  final String role;
  final void Function(int) onNavigate;
  const ErpHome({super.key, required this.role, required this.onNavigate});
  @override State<ErpHome> createState() => _ErpHomeState();
}

class _ErpHomeState extends State<ErpHome> {
  Map<String, dynamic> data = {};
  bool loading = true;

  bool get teacher => widget.role.toUpperCase().contains('TEACHER');
  bool get admin => widget.role.toUpperCase().contains('ADMIN');

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
      } else if (!admin) {
        data = await ApiService.instance.studentDashboard();
      } else {
        data = {};
      }
    } catch (e) {
      if (mounted) snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  String get displayName {
    final value = data['studentName'] ?? data['teacherName'];
    return value == null || value.toString().isEmpty ? 'Campus User' : value.toString();
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(22, 20, 22, 28),
        children: [
          Row(children: [
            CircleAvatar(radius: 25, backgroundColor: const Color(0xFFE0E7F8), child: Text(displayName.substring(0, 1).toUpperCase(), style: const TextStyle(color: navy, fontWeight: FontWeight.w900, fontSize: 19))),
            const SizedBox(width: 12),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Good to see you', style: TextStyle(color: muted, fontSize: 14)),
              Text(displayName, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
              Text(widget.role.toUpperCase(), style: const TextStyle(color: muted, fontSize: 10, letterSpacing: 1.1, fontWeight: FontWeight.w700)),
            ])),
            IconButton(onPressed: () => snack(context, 'Notifications will appear here.'), style: IconButton.styleFrom(backgroundColor: Colors.white, side: const BorderSide(color: Color(0xFFE6E1D9))), icon: const Icon(Icons.notifications_none_rounded)),
          ]),
          const SizedBox(height: 20),
          _hero(),
          const SizedBox(height: 22),
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            const Text('Quick overview', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
            Text(teacher ? 'Teaching' : 'This semester', style: const TextStyle(color: navy, fontWeight: FontWeight.w700)),
          ]),
          const SizedBox(height: 12),
          loading ? const SizedBox(height: 130, child: Center(child: CircularProgressIndicator())) : _stats(),
          const SizedBox(height: 22),
          const Text('Quick access', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
          const SizedBox(height: 12),
          _quickGrid(),
          const SizedBox(height: 20),
          _continueCard(),
        ],
      ),
    ),
  );

  Widget _hero() => Container(
    padding: const EdgeInsets.all(22),
    decoration: BoxDecoration(
      gradient: const LinearGradient(colors: [Color(0xFF315FBE), Color(0xFF304CA8)], begin: Alignment.topLeft, end: Alignment.bottomRight),
      borderRadius: BorderRadius.circular(28),
    ),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(teacher ? 'TEACHING WORKSPACE' : 'CAMPUS WORKSPACE', style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.2)),
      const SizedBox(height: 10),
      Text(teacher ? 'Manage your classes from one place.' : 'Everything you need for your semester.', style: const TextStyle(color: Colors.white, fontSize: 23, height: 1.2, fontWeight: FontWeight.w900)),
      const SizedBox(height: 7),
      Text(teacher ? 'Attendance, assignments, exams and students.' : 'Attendance, assignments, timetable, exams and results.', style: const TextStyle(color: Colors.white70, fontSize: 14, height: 1.45)),
      const SizedBox(height: 20),
      Wrap(spacing: 8, runSpacing: 8, children: [
        _heroChip(Icons.fact_check_outlined, 'Attendance'),
        _heroChip(Icons.assignment_outlined, 'Assignments'),
        _heroChip(Icons.calendar_month_outlined, 'Timetable'),
      ]),
    ]),
  );

  Widget _heroChip(IconData icon, String text) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
    decoration: BoxDecoration(color: Colors.white.withValues(alpha: .14), borderRadius: BorderRadius.circular(99), border: Border.all(color: Colors.white24)),
    child: Row(mainAxisSize: MainAxisSize.min, children: [Icon(icon, size: 15, color: Colors.white), const SizedBox(width: 6), Text(text, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w700))]),
  );

  Widget _stats() {
    final values = teacher
      ? [['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Students', data['totalStudents'] ?? 0, Icons.groups_outlined], ['Reviews', data['pendingReviewCount'] ?? 0, Icons.rate_review_outlined]]
      : [['Attendance', (data['attendancePercentage'] ?? 0).toString() + '%', Icons.fact_check_outlined], ['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Pending', data['pendingAssignments'] ?? 0, Icons.assignment_outlined]];
    return Row(crossAxisAlignment: CrossAxisAlignment.start, children: values.map((v) => Expanded(child: Padding(
      padding: const EdgeInsets.only(right: 8),
      child: Card(child: Padding(padding: const EdgeInsets.all(15), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(v[2] as IconData, color: navy, size: 20),
        const SizedBox(height: 9),
        Text(v[1].toString(), style: const TextStyle(fontSize: 23, fontWeight: FontWeight.w900)),
        Text(v[0].toString(), style: const TextStyle(color: muted, fontSize: 12)),
      ]))),
    ))).toList());
  }

  Widget _quickGrid() {
    final items = [
      ['Assignments', Icons.assignment_outlined],
      ['Attendance', Icons.fact_check_outlined],
      ['Timetable', Icons.calendar_month_outlined],
      ['Exams', Icons.event_note_outlined],
      ['Results', Icons.bar_chart_rounded],
      ['Profile', Icons.person_outline_rounded],
    ];
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: items.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 10, mainAxisSpacing: 10, childAspectRatio: 1.08),
      itemBuilder: (_, i) => InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: () => _open(items[i][0].toString()),
        child: Card(child: Padding(padding: const EdgeInsets.all(10), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
          CircleAvatar(radius: 22, backgroundColor: const Color(0xFFE8EAF6), child: Icon(items[i][1] as IconData, color: navy, size: 21)),
          const SizedBox(height: 8),
          Text(items[i][0].toString(), textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800)),
        ]))),
      ),
    );
  }

  void _open(String item) {
    if (item == 'Assignments') {
      Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherAssignmentsScreen() : const StudentAssignmentsScreen()));
      return;
    }
    if (item == 'Profile') {
      widget.onNavigate(4);
      return;
    }
    widget.onNavigate(1);
  }

  Widget _continueCard() => Card(
    child: Padding(padding: const EdgeInsets.all(18), child: Row(children: [
      Container(width: 48, height: 48, decoration: BoxDecoration(color: const Color(0xFFE7E8F2), borderRadius: BorderRadius.circular(16)), child: const Icon(Icons.bolt_rounded, color: navy)),
      const SizedBox(width: 13),
      const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Continue where you left off', style: TextStyle(color: navy, fontSize: 12, fontWeight: FontWeight.w900)),
        SizedBox(height: 4),
        Text('Open your academic workspace', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
      ])),
      const Icon(Icons.arrow_forward_ios_rounded, size: 16, color: muted),
    ])),
  );
}

class ErpAcademics extends StatelessWidget {
  final String role;
  const ErpAcademics({super.key, required this.role});

  bool get teacher => role.toUpperCase().contains('TEACHER');

  @override
  Widget build(BuildContext context) => SafeArea(
    child: ListView(padding: const EdgeInsets.fromLTRB(22, 24, 22, 30), children: [
      const Text('Academic workspace', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900)),
      const SizedBox(height: 5),
      const Text('Everything related to teaching and learning.', style: TextStyle(color: muted, fontSize: 15)),
      const SizedBox(height: 22),
      _feature(context, 'Assignments', 'Create, submit and evaluate assignments.', Icons.assignment_outlined, () => Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherAssignmentsScreen() : const StudentAssignmentsScreen()))),
      _feature(context, 'Attendance', 'View attendance and class participation.', Icons.fact_check_outlined, () => _info(context, 'Attendance', 'Attendance module is connected to the ERP backend and will be surfaced here.')),
      _feature(context, 'Timetable', 'See your daily classes and schedules.', Icons.calendar_month_outlined, () => _info(context, 'Timetable', 'Your timetable workspace will appear here.')),
      _feature(context, 'Exams & Results', 'Keep exam schedules and results together.', Icons.event_note_outlined, () => _info(context, 'Exams & Results', 'Exam and result views will use the ERP academic data.')),
    ]),
  );

  Widget _feature(BuildContext context, String title, String subtitle, IconData icon, VoidCallback tap) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: InkWell(borderRadius: BorderRadius.circular(24), onTap: tap, child: Card(child: Padding(padding: const EdgeInsets.all(18), child: Row(children: [
      Container(width: 52, height: 52, decoration: BoxDecoration(color: const Color(0xFFE7E8F2), borderRadius: BorderRadius.circular(17)), child: Icon(icon, color: navy)),
      const SizedBox(width: 15),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900)), const SizedBox(height: 4), Text(subtitle, style: const TextStyle(color: muted, height: 1.35))])),
      const Icon(Icons.chevron_right_rounded, color: muted),
    ])))),
  );

  void _info(BuildContext context, String title, String message) => showDialog(context: context, builder: (_) => AlertDialog(title: Text(title), content: Text(message), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close'))]));
}

class ErpAnalytics extends StatefulWidget {
  final String role;
  const ErpAnalytics({super.key, required this.role});
  @override State<ErpAnalytics> createState() => _ErpAnalyticsState();
}

class _ErpAnalyticsState extends State<ErpAnalytics> {
  Map<String, dynamic> data = {};
  bool loading = true;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() => loading = true);
    try {
      data = widget.role.toUpperCase().contains('TEACHER')
        ? await ApiService.instance.teacherDashboard()
        : await ApiService.instance.studentDashboard();
    } catch (e) {
      if (mounted) snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: RefreshIndicator(
      onRefresh: load,
      child: ListView(padding: const EdgeInsets.fromLTRB(22, 24, 22, 30), children: [
        Row(children: [
          const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Analytics', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900)), SizedBox(height: 5), Text('Track your academic progress over time', style: TextStyle(color: muted, fontSize: 15))])),
          IconButton(onPressed: load, style: IconButton.styleFrom(backgroundColor: Colors.white, side: const BorderSide(color: Color(0xFFE6E1D9))), icon: const Icon(Icons.refresh_rounded)),
        ]),
        const SizedBox(height: 24),
        const Text('Overview', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
        const SizedBox(height: 12),
        loading ? const SizedBox(height: 120, child: Center(child: CircularProgressIndicator())) : _overview(),
        const SizedBox(height: 18),
        Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Progress snapshot', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
          const SizedBox(height: 6),
          const Text('Use your attendance, assignments and results to understand your semester progress.', style: TextStyle(color: muted, height: 1.45)),
          const SizedBox(height: 18),
          _progress('Attendance', _attendance(), Icons.fact_check_outlined),
          const SizedBox(height: 15),
          _progress('Assignments', _assignmentProgress(), Icons.assignment_outlined),
        ]))),
        const SizedBox(height: 18),
        Card(child: Padding(padding: const EdgeInsets.all(20), child: Row(children: [
          const Icon(Icons.insights_rounded, color: green, size: 28),
          const SizedBox(width: 13),
          const Expanded(child: Text('Detailed charts will populate as academic activity accumulates.', style: TextStyle(color: muted, height: 1.4))),
        ]))),
      ]),
    ),
  );

  double _attendance() {
    final v = double.tryParse((data['attendancePercentage'] ?? 0).toString()) ?? 0;
    return (v / 100).clamp(0, 1);
  }

  double _assignmentProgress() {
    final pending = double.tryParse((data['pendingAssignments'] ?? 0).toString()) ?? 0;
    final total = double.tryParse((data['totalAssignments'] ?? 0).toString()) ?? 0;
    if (total <= 0) return 0;
    return ((total - pending) / total).clamp(0, 1);
  }

  Widget _overview() {
    final teacher = widget.role.toUpperCase().contains('TEACHER');
    final items = teacher
      ? [['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Students', data['totalStudents'] ?? 0, Icons.groups_outlined], ['Reviews', data['pendingReviewCount'] ?? 0, Icons.rate_review_outlined]]
      : [['Attendance', (data['attendancePercentage'] ?? 0).toString() + '%', Icons.fact_check_outlined], ['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Pending', data['pendingAssignments'] ?? 0, Icons.assignment_outlined]];
    return Row(crossAxisAlignment: CrossAxisAlignment.start, children: items.map((x) => Expanded(child: Padding(
      padding: const EdgeInsets.only(right: 8),
      child: Card(child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(x[2] as IconData, color: navy, size: 21),
        const SizedBox(height: 8),
        Text(x[1].toString(), style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900)),
        Text(x[0].toString(), style: const TextStyle(color: muted, fontSize: 12)),
      ]))),
    ))).toList());
  }

  Widget _progress(String title, double value, IconData icon) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [Icon(icon, size: 19, color: navy), const SizedBox(width: 8), Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.w800))), Text((value * 100).round().toString() + '%', style: const TextStyle(fontWeight: FontWeight.w800, color: navy))]),
    const SizedBox(height: 8),
    ClipRRect(borderRadius: BorderRadius.circular(20), child: LinearProgressIndicator(value: value, minHeight: 9, backgroundColor: const Color(0xFFE7E5DF), color: navy)),
  ]);
}

class ErpCommunity extends StatelessWidget {
  const ErpCommunity({super.key});

  @override
  Widget build(BuildContext context) => SafeArea(
    child: ListView(padding: const EdgeInsets.fromLTRB(22, 24, 22, 30), children: [
      const Text('Campus community', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900)),
      const SizedBox(height: 5),
      const Text('Stay connected with your college community.', style: TextStyle(color: muted, fontSize: 15)),
      const SizedBox(height: 20),
      TextField(decoration: const InputDecoration(hintText: 'Search announcements and sessions...', prefixIcon: Icon(Icons.search_rounded))),
      const SizedBox(height: 22),
      Row(children: [Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8), decoration: BoxDecoration(color: const Color(0xFFE9EAF5), borderRadius: BorderRadius.circular(99)), child: const Row(children: [Icon(Icons.star_rounded, color: navy, size: 17), SizedBox(width: 6), Text('Featured', style: TextStyle(color: navy, fontWeight: FontWeight.w800))])), const SizedBox(width: 9), const Text('Campus updates', style: TextStyle(color: muted))]),
      const SizedBox(height: 12),
      _post('Academic calendar', 'Stay updated with classes, examinations and semester activities.', Icons.calendar_month_rounded),
      _post('Student services', 'Important college services and resources in one mobile workspace.', Icons.support_agent_rounded),
      _post('Faculty updates', 'Teaching teams can share academic notices and reminders here.', Icons.campaign_outlined),
      const SizedBox(height: 10),
      Center(child: TextButton(onPressed: () => snack(context, 'More community updates will appear here.'), child: const Text('Load more'))),
    ]),
  );

  Widget _post(String title, String body, IconData icon) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Card(child: Padding(padding: const EdgeInsets.all(18), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Container(width: 46, height: 46, decoration: BoxDecoration(color: const Color(0xFFE7E8F2), borderRadius: BorderRadius.circular(15)), child: Icon(icon, color: navy)),
      const SizedBox(width: 13),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900)), const SizedBox(height: 5), Text(body, style: const TextStyle(color: muted, height: 1.4))])),
    ]))),
  );
}

class ErpProfile extends StatefulWidget {
  final String role;
  final Future<void> Function() onLogout;
  const ErpProfile({super.key, required this.role, required this.onLogout});
  @override State<ErpProfile> createState() => _ErpProfileState();
}

class _ErpProfileState extends State<ErpProfile> {
  Map<String, dynamic> profile = {};
  bool loading = true;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() => loading = true);
    try {
      final value = await ApiService.instance.request('/users/me');
      if (value is Map) profile = Map<String, dynamic>.from(value);
    } catch (e) {
      if (mounted) snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final email = (profile['email'] ?? 'Campus account').toString();
    final role = (profile['role'] ?? widget.role).toString();
    final tenant = (profile['tenant'] ?? 'EduSphere ERP').toString();
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: load,
        child: ListView(padding: const EdgeInsets.fromLTRB(22, 24, 22, 30), children: [
          Row(children: [
            const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('My Profile', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900)), SizedBox(height: 5), Text('Manage your campus account', style: TextStyle(color: muted, fontSize: 15))])),
            TextButton(onPressed: load, child: const Text('Refresh')),
          ]),
          const SizedBox(height: 20),
          Card(child: Padding(padding: const EdgeInsets.all(24), child: Column(children: [
            CircleAvatar(radius: 48, backgroundColor: const Color(0xFF3D70CA), child: Text(email.isEmpty ? 'U' : email.substring(0, 1).toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 36, fontWeight: FontWeight.w700))),
            const SizedBox(height: 14),
            Text(email, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900), textAlign: TextAlign.center),
            const SizedBox(height: 5),
            Text(role.toUpperCase(), style: const TextStyle(color: muted, fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1)),
          ]))),
          const SizedBox(height: 14),
          Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Row(children: [Icon(Icons.person_outline_rounded, color: navy), SizedBox(width: 10), Text('Account details', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900))]),
            const SizedBox(height: 18),
            _detail('Email', email, Icons.mail_outline_rounded),
            _detail('Role', role, Icons.badge_outlined),
            _detail('College / Tenant', tenant, Icons.account_balance_outlined),
          ]))),
          const SizedBox(height: 14),
          Card(child: Column(children: [
            ListTile(leading: const Icon(Icons.lock_reset_rounded, color: navy), title: const Text('Change password', style: TextStyle(fontWeight: FontWeight.w800)), trailing: const Icon(Icons.chevron_right_rounded), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ChangePasswordScreen(onComplete: () => Navigator.pop(context))))),
            const Divider(height: 1),
            ListTile(leading: const Icon(Icons.logout_rounded, color: Colors.redAccent), title: const Text('Sign out', style: TextStyle(fontWeight: FontWeight.w800)), onTap: widget.onLogout),
          ])),
        ]),
      ),
    );
  }

  Widget _detail(String title, String value, IconData icon) => Padding(
    padding: const EdgeInsets.only(bottom: 13),
    child: Row(children: [Icon(icon, size: 20, color: muted), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(color: muted, fontSize: 12)), const SizedBox(height: 2), Text(value, style: const TextStyle(fontWeight: FontWeight.w700))]))]),
  );
}
