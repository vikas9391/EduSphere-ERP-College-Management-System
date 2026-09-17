part of 'main.dart';

class ProfileScreen extends StatefulWidget {
  final String role;
  const ProfileScreen({super.key, required this.role});
  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final phone = TextEditingController();
  final address = TextEditingController();
  final city = TextEditingController();
  final state = TextEditingController();
  final pincode = TextEditingController();
  final parentPhone = TextEditingController();
  final parentEmail = TextEditingController();
  final bloodGroup = TextEditingController();
  Map<String, dynamic> profile = {};
  bool loading = true;
  bool saving = false;

  @override
  void initState() { super.initState(); _load(); }

  @override
  void dispose() {
    for (final c in [phone, address, city, state, pincode, parentPhone, parentEmail, bloodGroup]) c.dispose();
    super.dispose();
  }

  void _set(TextEditingController controller, dynamic value) => controller.text = value?.toString() ?? '';

  Future<void> _load() async {
    try {
      profile = await ApiService.instance.map('/student/profile');
      _set(phone, profile['phone']); _set(address, profile['address']); _set(city, profile['city']);
      _set(state, profile['state']); _set(pincode, profile['pincode']); _set(parentPhone, profile['parentPhone']);
      _set(parentEmail, profile['parentEmail']); _set(bloodGroup, profile['bloodGroup']);
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => loading = false); }
  }

  Future<void> _save() async {
    setState(() => saving = true);
    try {
      profile = await ApiService.instance.mapPut('/student/profile', {
        'phone': phone.text.trim(), 'address': address.text.trim(), 'city': city.text.trim(),
        'state': state.text.trim(), 'pincode': pincode.text.trim(), 'parentPhone': parentPhone.text.trim(),
        'parentEmail': parentEmail.text.trim(), 'bloodGroup': bloodGroup.text.trim(),
      });
      if (mounted) snack(context, 'Profile updated successfully');
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => saving = false); }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.role.toUpperCase().contains('STUDENT')) {
      return Scaffold(appBar: AppBar(title: const Text('Profile')), body: const Center(child: Text('Profile management is currently available for students.')));
    }
    return Scaffold(
      appBar: AppBar(title: const Text('My Profile')),
      body: loading ? const Center(child: CircularProgressIndicator()) : RefreshIndicator(
        onRefresh: _load,
        child: ListView(padding: const EdgeInsets.all(20), children: [
          Card(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('${profile['firstName'] ?? ''} ${profile['lastName'] ?? ''}'.trim(), style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w900)),
            Text('${profile['admissionNo'] ?? '—'} · ${profile['email'] ?? '—'}'),
            const SizedBox(height: 8), Text('${profile['courseName'] ?? 'Course'} · ${profile['department'] ?? 'Department'}'),
          ]))),
          _section('Contact', [phone, address, city, state, pincode], ['Phone', 'Address', 'City', 'State', 'Pincode']),
          _section('Parent', [parentPhone, parentEmail], ['Parent phone', 'Parent email']),
          _section('Personal', [bloodGroup], ['Blood group']),
          const SizedBox(height: 20),
          SizedBox(height: 52, child: FilledButton.icon(onPressed: saving ? null : _save, icon: const Icon(Icons.save_outlined), label: Text(saving ? 'Saving…' : 'Save profile'))),
        ]),
      ),
    );
  }

  Widget _section(String title, List<TextEditingController> controllers, List<String> labels) => Padding(
    padding: const EdgeInsets.only(top: 12),
    child: Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)), const SizedBox(height: 12),
      for (var i = 0; i < controllers.length; i++) Padding(padding: const EdgeInsets.only(bottom: 10), child: TextField(controller: controllers[i], decoration: InputDecoration(labelText: labels[i]))),
    ]))),
  );
}

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});
  @override State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  List<Map<String, dynamic>> items = [];
  bool loading = true;
  @override void initState() { super.initState(); _load(); }
  Future<void> _load() async {
    try { items = await ApiService.instance.list('/announcements'); }
    catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => loading = false); }
  }
  Future<void> _read(Map<String, dynamic> item) async {
    final id = item['id']; if (id == null || item['read'] == true) return;
    try { await ApiService.instance.request('/announcements/$id/read', method: 'POST'); if (mounted) setState(() => item['read'] = true); }
    catch (e) { if (mounted) snack(context, cleanError(e)); }
  }
  Future<void> _readAll() async {
    try { await ApiService.instance.request('/announcements/read-all', method: 'POST'); if (mounted) setState(() { for (final item in items) item['read'] = true; }); }
    catch (e) { if (mounted) snack(context, cleanError(e)); }
  }
  @override
  Widget build(BuildContext context) {
    final unread = items.where((item) => item['read'] != true).length;
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications'), actions: [if (unread > 0) TextButton(onPressed: _readAll, child: const Text('Read all'))]),
      body: loading ? const Center(child: CircularProgressIndicator()) : RefreshIndicator(
        onRefresh: _load,
        child: items.isEmpty ? ListView(children: const [SizedBox(height: 180), Center(child: Text('No notifications'))]) : ListView.builder(
          padding: const EdgeInsets.all(16), itemCount: items.length,
          itemBuilder: (context, index) {
            final item = items[index]; final read = item['read'] == true;
            return Card(color: read ? null : const Color(0xFFE8F5E9), child: ListTile(
              onTap: () => _read(item), leading: CircleAvatar(child: Icon(read ? Icons.notifications_none : Icons.notifications)),
              title: Text('${item['title'] ?? 'Announcement'}', style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text('${item['message'] ?? ''}'), trailing: read ? null : const Icon(Icons.circle, size: 10),
            ));
          },
        ),
      ),
    );
  }
}
