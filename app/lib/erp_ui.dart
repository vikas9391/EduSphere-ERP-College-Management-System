part of 'main.dart';

class ErpShell extends StatefulWidget {
  final String role;
  final Future<void> Function() onLogout;
  const ErpShell({super.key, required this.role, required this.onLogout});
  @override State<ErpShell> createState() => _ErpShellState();
}

class _ErpShellState extends State<ErpShell> {
  int index = 0;

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
      backgroundColor: pageBg,
      body: IndexedStack(index: index, children: pages),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(14, 0, 14, 12),
        child: Container(
          padding: const EdgeInsets.all(7),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: border),
            boxShadow: const [BoxShadow(color: Color(0x0D1F2937), blurRadius: 18, offset: Offset(0, 7))],
          ),
          child: Row(children: [
            _nav(0, Icons.dashboard_outlined, 'Home'),
            _nav(1, Icons.menu_book_outlined, 'Academics'),
            _nav(2, Icons.insights_outlined, 'Analytics'),
            _nav(3, Icons.campaign_outlined, 'Community'),
            _nav(4, Icons.person_outline_rounded, 'Profile'),
          ]),
        ),
      ),
    );
  }

  Widget _nav(int value, IconData icon, String label) {
    final active = index == value;
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => select(value),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: active ? lightGreen : Colors.transparent,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icon, size: 21, color: active ? navy : muted),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(fontSize: 10.5, fontWeight: active ? FontWeight.w700 : FontWeight.w500, color: active ? navy : muted)),
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
      color: navy,
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
        children: [
          _topBar(),
          const SizedBox(height: 18),
          _welcomeCard(),
          const SizedBox(height: 24),
          _sectionTitle('Overview', teacher ? 'Teaching' : 'This semester'),
          const SizedBox(height: 12),
          loading ? const SizedBox(height: 116, child: Center(child: CircularProgressIndicator())) : _stats(),
          const SizedBox(height: 24),
          _sectionTitle('Quick access', 'View all'),
          const SizedBox(height: 12),
          _quickGrid(),
          const SizedBox(height: 20),
          _noticeCard(),
        ],
      ),
    ),
  );

  Widget _topBar() => Row(children: [
    Container(
      width: 43, height: 43,
      decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(14)),
      child: const Icon(Icons.spa_rounded, color: navy, size: 25),
    ),
    const SizedBox(width: 11),
    const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('EduSphere', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: textColor)),
      Text('Smart Campus, Smarter Future', style: TextStyle(fontSize: 10.5, color: muted)),
    ])),
    IconButton(
      onPressed: () => snack(context, 'Notifications will appear here.'),
      style: IconButton.styleFrom(backgroundColor: Colors.white, side: const BorderSide(color: border)),
      icon: const Icon(Icons.notifications_none_rounded, color: textColor),
    ),
  ]);

  Widget _welcomeCard() => Container(
    padding: const EdgeInsets.all(21),
    decoration: BoxDecoration(
      color: lightGreen,
      borderRadius: BorderRadius.circular(24),
      border: Border.all(color: const Color(0xFFDDEDDD)),
    ),
    child: Row(children: [
      CircleAvatar(
        radius: 27,
        backgroundColor: Colors.white,
        child: Text(displayName.substring(0, 1).toUpperCase(), style: const TextStyle(color: navy, fontSize: 20, fontWeight: FontWeight.w800)),
      ),
      const SizedBox(width: 13),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Text('Welcome back', style: TextStyle(color: muted, fontSize: 12, fontWeight: FontWeight.w600)),
        const SizedBox(height: 2),
        Text(displayName, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: textColor, fontSize: 21, fontWeight: FontWeight.w800)),
        const SizedBox(height: 2),
        Text(widget.role.toUpperCase(), style: const TextStyle(color: navy, fontSize: 10, letterSpacing: 1, fontWeight: FontWeight.w700)),
      ])),
    ]),
  );

  Widget _sectionTitle(String title, String action) => Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [
      Text(title, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800, color: textColor)),
      Text(action, style: const TextStyle(color: navy, fontSize: 12, fontWeight: FontWeight.w700)),
    ],
  );

  Widget _stats() {
    final values = teacher
      ? [['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Students', data['totalStudents'] ?? 0, Icons.groups_outlined], ['Reviews', data['pendingReviewCount'] ?? 0, Icons.rate_review_outlined]]
      : [['Attendance', (data['attendancePercentage'] ?? 0).toString() + '%', Icons.fact_check_outlined], ['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Pending', data['pendingAssignments'] ?? 0, Icons.assignment_outlined]];
    return Row(children: values.map((v) => Expanded(child: Padding(
      padding: const EdgeInsets.only(right: 8),
      child: _statCard(v[0].toString(), v[1].toString(), v[2] as IconData),
    ))).toList());
  }

  Widget _statCard(String label, String value, IconData icon) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), border: Border.all(color: border)),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Container(width: 36, height: 36, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(11)), child: Icon(icon, color: navy, size: 19)),
      const SizedBox(height: 10),
      Text(value, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800, color: textColor)),
      const SizedBox(height: 2),
      Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: muted)),
    ]),
  );

  Widget _quickGrid() {
    final items = [
      ['Assignments', Icons.assignment_outlined],
      ['Attendance', Icons.fact_check_outlined],
      ['Timetable', Icons.calendar_month_outlined],
      ['Exams', Icons.event_note_outlined],
      ['Results', Icons.emoji_events_outlined],
      ['Profile', Icons.person_outline_rounded],
    ];
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: items.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 10, mainAxisSpacing: 10, childAspectRatio: 1.05),
      itemBuilder: (_, i) => InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: () => _open(items[i][0].toString()),
        child: Container(
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), border: Border.all(color: border)),
          padding: const EdgeInsets.all(10),
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            Container(width: 43, height: 43, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(14)), child: Icon(items[i][1] as IconData, color: navy, size: 21)),
            const SizedBox(height: 8),
            Text(items[i][0].toString(), textAlign: TextAlign.center, style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: textColor)),
          ]),
        ),
      ),
    );
  }

  Future<void> _open(String item) async {
    if (item == 'Assignments') {
      await _showBookLoader(context, 'Opening assignments…');
      if (!mounted) return;
      Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherAssignmentsScreen() : const StudentAssignmentsScreen()));
    } else if (item == 'Profile') {
      widget.onNavigate(4);
    } else {
      widget.onNavigate(1);
    }
  }

  Future<void> _showBookLoader(BuildContext context, String label) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      barrierColor: pageBg,
      builder: (dialogContext) {
        Future.delayed(const Duration(milliseconds: 520), () {
          if (dialogContext.mounted && Navigator.of(dialogContext).canPop()) Navigator.of(dialogContext).pop();
        });
        return BookLoadingScreen(label: label);
      },
    );
  }

  Widget _noticeCard() => Container(
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(22), border: Border.all(color: border)),
    child: Row(children: [
      Container(width: 43, height: 43, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(13)), child: const Icon(Icons.spa_outlined, color: navy)),
      const SizedBox(width: 12),
      const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Your campus workspace', style: TextStyle(fontSize: 13, color: navy, fontWeight: FontWeight.w800)),
        SizedBox(height: 4),
        Text('Everything you need for teaching and learning, in one place.', style: TextStyle(fontSize: 14, color: textColor, fontWeight: FontWeight.w600)),
      ])),
    ]),
  );
}

class ErpAcademics extends StatelessWidget {
  final String role;
  const ErpAcademics({super.key, required this.role});
  bool get teacher => role.toUpperCase().contains('TEACHER');

  @override
  Widget build(BuildContext context) => SafeArea(
    child: ListView(padding: const EdgeInsets.fromLTRB(20, 20, 20, 30), children: [
      _pageHeader('Academics', 'Teaching and learning workspace'),
      const SizedBox(height: 20),
      _feature(context, 'Assignments', 'Create, submit and evaluate assignments.', Icons.assignment_outlined, () => Navigator.push(context, MaterialPageRoute(builder: (_) => teacher ? const TeacherAssignmentsScreen() : const StudentAssignmentsScreen()))),
      _feature(context, 'Attendance', 'View attendance and class participation.', Icons.fact_check_outlined, () => _info(context, 'Attendance', 'Attendance data is connected to the ERP backend.')),
      _feature(context, 'Timetable', 'See your daily classes and schedules.', Icons.calendar_month_outlined, () => _info(context, 'Timetable', 'Your timetable workspace will appear here.')),
      _feature(context, 'Exams & Results', 'Keep exam schedules and results together.', Icons.event_note_outlined, () => _info(context, 'Exams & Results', 'Exam and result views will use the ERP academic data.')),
    ]),
  );

  Widget _pageHeader(String title, String subtitle) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text(title, style: const TextStyle(fontSize: 29, fontWeight: FontWeight.w800, color: textColor)),
    const SizedBox(height: 5),
    Text(subtitle, style: const TextStyle(color: muted, fontSize: 14)),
  ]);

  Widget _feature(BuildContext context, String title, String subtitle, IconData icon, VoidCallback tap) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: InkWell(
      borderRadius: BorderRadius.circular(22),
      onTap: tap,
      child: Container(
        padding: const EdgeInsets.all(17),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(22), border: Border.all(color: border)),
        child: Row(children: [
          Container(width: 50, height: 50, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(16)), child: Icon(icon, color: navy)),
          const SizedBox(width: 14),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: textColor)),
            const SizedBox(height: 4),
            Text(subtitle, style: const TextStyle(color: muted, height: 1.35, fontSize: 13)),
          ])),
          const Icon(Icons.chevron_right_rounded, color: muted),
        ]),
      ),
    ),
  );

  void _info(BuildContext context, String title, String message) => showDialog(context: context, builder: (_) => AlertDialog(
    title: Text(title),
    content: Text(message),
    actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close'))],
  ));
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
  void initState() { super.initState(); load(); }

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
      color: navy,
      onRefresh: load,
      child: ListView(padding: const EdgeInsets.fromLTRB(20, 20, 20, 30), children: [
        _header(),
        const SizedBox(height: 20),
        loading ? const SizedBox(height: 120, child: Center(child: CircularProgressIndicator())) : _overview(),
        const SizedBox(height: 14),
        _progressCard(),
      ]),
    ),
  );

  Widget _header() => Row(children: [
    const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Analytics', style: TextStyle(fontSize: 29, fontWeight: FontWeight.w800, color: textColor)),
      SizedBox(height: 5),
      Text('Track your academic progress over time', style: TextStyle(color: muted, fontSize: 14)),
    ])),
    IconButton(onPressed: load, style: IconButton.styleFrom(backgroundColor: Colors.white, side: const BorderSide(color: border)), icon: const Icon(Icons.refresh_rounded)),
  ]);

  Widget _overview() {
    final teacher = widget.role.toUpperCase().contains('TEACHER');
    final items = teacher
      ? [['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Students', data['totalStudents'] ?? 0, Icons.groups_outlined], ['Reviews', data['pendingReviewCount'] ?? 0, Icons.rate_review_outlined]]
      : [['Attendance', (data['attendancePercentage'] ?? 0).toString() + '%', Icons.fact_check_outlined], ['Subjects', data['totalSubjects'] ?? 0, Icons.menu_book_outlined], ['Pending', data['pendingAssignments'] ?? 0, Icons.assignment_outlined]];
    return Row(children: items.map((x) => Expanded(child: Padding(
      padding: const EdgeInsets.only(right: 8),
      child: _statCard(x[0].toString(), x[1].toString(), x[2] as IconData),
    ))).toList());
  }

  Widget _statCard(String label, String value, IconData icon) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: border),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: lightGreen,
            borderRadius: BorderRadius.circular(11),
          ),
          child: Icon(icon, color: navy, size: 19),
        ),
        const SizedBox(height: 10),
        Text(value, style: const TextStyle(
          fontSize: 21,
          fontWeight: FontWeight.w800,
          color: textColor,
        )),
        const SizedBox(height: 2),
        Text(label, maxLines: 1, overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 11, color: muted)),
      ],
    ),
  );

  Widget _progressCard() => Container(
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(24), border: Border.all(color: border)),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('Progress snapshot', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: textColor)),
      const SizedBox(height: 5),
      const Text('Use your attendance and assignment activity to understand your semester progress.', style: TextStyle(color: muted, height: 1.4, fontSize: 13)),
      const SizedBox(height: 20),
      _progress('Attendance', _attendance(), Icons.fact_check_outlined),
      const SizedBox(height: 18),
      _progress('Assignments', _assignmentProgress(), Icons.assignment_outlined),
    ]),
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

  Widget _progress(String title, double value, IconData icon) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [
      Container(width: 34, height: 34, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(10)), child: Icon(icon, size: 17, color: navy)),
      const SizedBox(width: 9),
      Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.w700, color: textColor))),
      Text((value * 100).round().toString() + '%', style: const TextStyle(fontWeight: FontWeight.w800, color: navy)),
    ]),
    const SizedBox(height: 9),
    ClipRRect(borderRadius: BorderRadius.circular(20), child: LinearProgressIndicator(value: value, minHeight: 9, backgroundColor: const Color(0xFFE8EEE5), color: green)),
  ]);
}

class ErpCommunity extends StatelessWidget {
  const ErpCommunity({super.key});

  @override
  Widget build(BuildContext context) => SafeArea(
    child: ListView(padding: const EdgeInsets.fromLTRB(20, 20, 20, 30), children: [
      const Text('Community', style: TextStyle(fontSize: 29, fontWeight: FontWeight.w800, color: textColor)),
      const SizedBox(height: 5),
      const Text('Stay connected with your college community.', style: TextStyle(color: muted, fontSize: 14)),
      const SizedBox(height: 18),
      const TextField(decoration: InputDecoration(hintText: 'Search announcements and sessions...', prefixIcon: Icon(Icons.search_rounded))),
      const SizedBox(height: 18),
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
        decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(14)),
        child: const Row(mainAxisSize: MainAxisSize.min, children: [Icon(Icons.spa_rounded, color: navy, size: 17), SizedBox(width: 7), Text('Campus updates', style: TextStyle(color: navy, fontWeight: FontWeight.w700))]),
      ),
      const SizedBox(height: 12),
      _post('Academic calendar', 'Stay updated with classes, examinations and semester activities.', Icons.calendar_month_rounded),
      _post('Student services', 'Important college services and resources in one mobile workspace.', Icons.support_agent_rounded),
      _post('Faculty updates', 'Teaching teams can share academic notices and reminders here.', Icons.campaign_outlined),
    ]),
  );

  Widget _post(String title, String body, IconData icon) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Container(
      padding: const EdgeInsets.all(17),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(22), border: Border.all(color: border)),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Container(width: 45, height: 45, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(14)), child: Icon(icon, color: navy)),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800, color: textColor)),
          const SizedBox(height: 5),
          Text(body, style: const TextStyle(color: muted, height: 1.4, fontSize: 13)),
        ])),
      ]),
    ),
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
  void initState() { super.initState(); load(); }

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
    final initial = email.isEmpty ? 'U' : email.substring(0, 1).toUpperCase();
    return SafeArea(
      child: RefreshIndicator(
        color: navy,
        onRefresh: load,
        child: ListView(padding: const EdgeInsets.fromLTRB(20, 20, 20, 30), children: [
          const Text('My Profile', style: TextStyle(fontSize: 29, fontWeight: FontWeight.w800, color: textColor)),
          const SizedBox(height: 5),
          const Text('Manage your campus account', style: TextStyle(color: muted, fontSize: 14)),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(24), border: Border.all(color: const Color(0xFFDDEDDD))),
            child: Column(children: [
              CircleAvatar(radius: 46, backgroundColor: Colors.white, child: Text(initial, style: const TextStyle(color: navy, fontSize: 32, fontWeight: FontWeight.w800))),
              const SizedBox(height: 13),
              Text(email, textAlign: TextAlign.center, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: textColor)),
              const SizedBox(height: 4),
              Text(role.toUpperCase(), style: const TextStyle(color: navy, fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: 1)),
            ]),
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(24), border: Border.all(color: border)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Row(children: [Icon(Icons.person_outline_rounded, color: navy), SizedBox(width: 9), Text('Account details', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: textColor))]),
              const SizedBox(height: 18),
              _detail('Email', email, Icons.mail_outline_rounded),
              _detail('Role', role, Icons.badge_outlined),
              _detail('College / Tenant', tenant, Icons.account_balance_outlined),
            ]),
          ),
          const SizedBox(height: 14),
          Container(
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(24), border: Border.all(color: border)),
            child: Column(children: [
              ListTile(
                leading: Container(width: 38, height: 38, decoration: BoxDecoration(color: lightGreen, borderRadius: BorderRadius.circular(11)), child: const Icon(Icons.lock_reset_rounded, color: navy, size: 20)),
                title: const Text('Change password', style: TextStyle(fontWeight: FontWeight.w700, color: textColor)),
                trailing: const Icon(Icons.chevron_right_rounded, color: muted),
                onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ChangePasswordScreen(onComplete: () => Navigator.pop(context)))),
              ),
              const Divider(height: 1, color: border),
              ListTile(
                leading: Container(width: 38, height: 38, decoration: BoxDecoration(color: const Color(0xFFFFF1EE), borderRadius: BorderRadius.circular(11)), child: const Icon(Icons.logout_rounded, color: Color(0xFFC1543C), size: 20)),
                title: const Text('Sign out', style: TextStyle(fontWeight: FontWeight.w700, color: textColor)),
                onTap: widget.onLogout,
              ),
            ]),
          ),
        ]),
      ),
    );
  }

  Widget _detail(String title, String value, IconData icon) => Padding(
    padding: const EdgeInsets.only(bottom: 14),
    child: Row(children: [
      Icon(icon, size: 19, color: muted),
      const SizedBox(width: 11),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(color: muted, fontSize: 11)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w700, color: textColor)),
      ])),
    ]),
  );
}
