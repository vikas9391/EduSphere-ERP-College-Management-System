part of 'main.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});
  @override State<ForgotPasswordScreen> createState() => _ForgotPasswordState();
}
class _ForgotPasswordState extends State<ForgotPasswordScreen> {
  final college = TextEditingController();
  final email = TextEditingController();
  bool busy = false, sent = false;
  @override void dispose() { college.dispose(); email.dispose(); super.dispose(); }
  Future<void> submit() async {
    if (college.text.trim().isEmpty || email.text.trim().isEmpty) { snack(context, 'Enter your college code and email.'); return; }
    setState(() => busy = true);
    try {
      await ApiService.instance.request('/auth/forgot-password', method: 'POST', retry: false, body: {'collegeCode': college.text.trim(), 'email': email.text.trim()});
      if (mounted) setState(() => sent = true);
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => busy = false); }
  }
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Forgot password')),
    body: ListView(padding: const EdgeInsets.all(24), children: [
      const SizedBox(height: 30),
      const Icon(Icons.lock_reset, size: 64, color: green),
      const SizedBox(height: 18),
      Text(sent ? 'Check your email' : 'Reset your password', style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900)),
      const SizedBox(height: 8),
      Text(sent ? 'If an account exists for this email, a password reset link has been sent.' : 'Enter the same college code and email you use to sign in.', style: const TextStyle(color: Colors.black54)),
      const SizedBox(height: 24),
      if (!sent) ...[
        TextField(controller: college, decoration: const InputDecoration(labelText: 'College code')),
        const SizedBox(height: 12),
        TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email')),
        const SizedBox(height: 20),
        SizedBox(height: 52, child: FilledButton(onPressed: busy ? null : submit, child: busy ? const CircularProgressIndicator(color: Colors.white) : const Text('Send reset link'))),
      ] else
        OutlinedButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.arrow_back), label: const Text('Back to sign in')),
    ]),
  );
}
