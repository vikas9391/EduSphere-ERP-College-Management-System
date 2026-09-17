import 'package:flutter/material.dart';
import 'main.dart';

class StudentExamsScreen extends StatelessWidget {
  const StudentExamsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('My Exams')),
      body: FutureBuilder<List<Map<String, dynamic>>>(
        future: ApiService.instance.list('/exam-schedules/my'),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) return ErrorView(cleanError(snapshot.error!));
          final rows = snapshot.data ?? [];
          if (rows.isEmpty) {
            return const Center(child: Text('No exam schedule available.'));
          }
          return RefreshIndicator(
            onRefresh: () async {
              await ApiService.instance.list('/exam-schedules/my');
            },
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: rows.length,
              itemBuilder: (context, index) {
                final x = rows[index];
                final date = '${x['examDate'] ?? '—'}';
                final start = '${x['startTime'] ?? ''}';
                final end = '${x['endTime'] ?? ''}';
                final time = start.isEmpty && end.isEmpty ? 'Time not specified' : '$start${end.isEmpty ? '' : ' – $end'}';
                return Card(
                  child: ListTile(
                    leading: const CircleAvatar(child: Icon(Icons.event_note_outlined)),
                    title: Text('${x['subjectName'] ?? 'Subject'}', style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Text('${x['examName'] ?? 'Exam'}\n$date · $time\nRoom: ${x['room'] ?? '—'}'),
                    isThreeLine: true,
                    trailing: x['maxMarks'] == null ? null : Text('${x['maxMarks']} marks', style: const TextStyle(fontWeight: FontWeight.w700)),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
