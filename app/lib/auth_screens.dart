part of 'main.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});
  @override State<ForgotPasswordScreen> createState() => _ForgotPasswordState();
}

class _ForgotPasswordState extends State<ForgotPasswordScreen> {
  final college = TextEditingController();
  final email = TextEditingController();
  bool busy = false, sent = false;

  @override
  void dispose() { college.dispose(); email.dispose(); super.dispose(); }

  Future<void> submit() async {
    if (college.text.trim().isEmpty || email.text.trim().isEmpty) {
      snack(context, 'Enter your college code and email.');
      return;
    }
    setState(() => busy = true);
    try {
      await ApiService.instance.request('/auth/forgot-password', method: 'POST', retry: false, body: {'collegeCode': college.text.trim(), 'email': email.text.trim()});
      if (mounted) setState(() => sent = true);
    } catch (e) {
      if (mounted) snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Forgot password')),
    body: ListView(padding: const EdgeInsets.all(24), children: [
      const SizedBox(height: 30),
      const Icon(Icons.lock_reset, size: 64, color: green),
      const SizedBox(height: 18),
      Text(sent ? 'Check your email' : 'Reset your password', style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900)),
      const SizedBox(height: 8),
      Text(sent ? 'If an account exists for this email, a password reset link has been sent.' : 'Enter the same college code and email you use to sign in.', style: const TextStyle(color: muted)),
      const SizedBox(height: 24),
      if (!sent) ...[
        TextField(controller: college, decoration: const InputDecoration(labelText: 'College code', prefixIcon: Icon(Icons.account_balance_outlined))),
        const SizedBox(height: 12),
        TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email', prefixIcon: Icon(Icons.mail_outline))),
        const SizedBox(height: 20),
        SizedBox(height: 52, child: FilledButton(onPressed: busy ? null : submit, child: busy ? const CircularProgressIndicator(color: Colors.white) : const Text('Send reset link'))),
      ] else
        OutlinedButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.arrow_back), label: const Text('Back to sign in')),
    ]),
  );
}

class ChangePasswordScreen extends StatefulWidget {
  final VoidCallback? onComplete;
  const ChangePasswordScreen({super.key, this.onComplete});
  @override State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final current = TextEditingController();
  final next = TextEditingController();
  final confirm = TextEditingController();
  bool busy = false;
  bool obscure = true;

  @override
  void dispose() {
    current.dispose();
    next.dispose();
    confirm.dispose();
    super.dispose();
  }

  Future<void> save() async {
    if (current.text.isEmpty || next.text.isEmpty || confirm.text.isEmpty) {
      snack(context, 'Complete all password fields.');
      return;
    }
    if (next.text.length < 8) {
      snack(context, 'New password must be at least 8 characters.');
      return;
    }
    if (next.text != confirm.text) {
      snack(context, 'New passwords do not match.');
      return;
    }
    setState(() => busy = true);
    try {
      await ApiService.instance.request('/users/me/password', method: 'PUT', body: {'currentPassword': current.text, 'newPassword': next.text});
      final p = await SharedPreferences.getInstance();
      await p.setBool('mustChangePassword', false);
      if (mounted) {
        snack(context, 'Password changed successfully.');
        widget.onComplete?.call();
      }
    } catch (e) {
      if (mounted) snack(context, cleanError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Change password')),
    body: ListView(padding: const EdgeInsets.all(24), children: [
      const SizedBox(height: 25),
      const Icon(Icons.lock_outline_rounded, size: 58, color: navy),
      const SizedBox(height: 16),
      const Text('Secure your account', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900)),
      const SizedBox(height: 7),
      const Text('Choose a new password for your EduSphere account.', style: TextStyle(color: muted)),
      const SizedBox(height: 25),
      TextField(controller: current, obscureText: obscure, decoration: const InputDecoration(labelText: 'Current password', prefixIcon: Icon(Icons.lock_outline))),
      const SizedBox(height: 12),
      TextField(controller: next, obscureText: obscure, decoration: InputDecoration(labelText: 'New password', prefixIcon: const Icon(Icons.password_outlined), suffixIcon: IconButton(onPressed: () => setState(() => obscure = !obscure), icon: Icon(obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined)))),
      const SizedBox(height: 12),
      TextField(controller: confirm, obscureText: obscure, decoration: const InputDecoration(labelText: 'Confirm new password', prefixIcon: Icon(Icons.verified_user_outlined))),
      const SizedBox(height: 20),
      SizedBox(height: 54, child: FilledButton(onPressed: busy ? null : save, child: busy ? const CircularProgressIndicator(color: Colors.white) : const Text('Save password', style: TextStyle(fontWeight: FontWeight.w800)))),
    ]),
  );
}
