part of 'main.dart';

class TeacherExamsScreen extends StatelessWidget {
  const TeacherExamsScreen({super.key});
  @override Widget build(BuildContext context) => FutureBuilder<List<Map<String,dynamic>>>(
    future: ApiService.instance.list('/exam-schedules/mine'),
    builder: (context, snapshot) {
      if(snapshot.connectionState==ConnectionState.waiting)return const Scaffold(body:Center(child:CircularProgressIndicator()));
      if(snapshot.hasError)return Scaffold(appBar:AppBar(title:const Text('My Exams')),body:ErrorView(cleanError(snapshot.error!)));
      final rows=snapshot.data??[];
      return Scaffold(appBar:AppBar(title:const Text('My Exams')),body:rows.isEmpty?const Center(child:Text('No exam schedules assigned.')):RefreshIndicator(onRefresh:()async{await ApiService.instance.list('/exam-schedules/mine');},child:ListView.builder(padding:const EdgeInsets.all(16),itemCount:rows.length,itemBuilder:(c,i){final x=rows[i];return Card(child:ListTile(leading:const CircleAvatar(child:Icon(Icons.event_note_outlined)),title:Text((x['examName']??'Exam').toString()+' · '+(x['subjectName']??'Subject').toString(),style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text((x['className']??'Class').toString()+' · '+(x['examDate']??'').toString()+' · '+(x['startTime']??'').toString()+'-'+(x['endTime']??'').toString()+'\\nRoom '+(x['room']??'—').toString()+' · Max '+(x['maxMarks']??'—').toString()),isThreeLine:true,trailing:const Icon(Icons.chevron_right),onTap:()=>Navigator.push(c,MaterialPageRoute(builder:(_)=>TeacherMarksScreen(schedule:x)))));})));}
    );
}
class TeacherMarksScreen extends StatefulWidget {
  final Map<String,dynamic> schedule;
  const TeacherMarksScreen({super.key,required this.schedule});
  @override State<TeacherMarksScreen> createState()=>_TeacherMarksState();
}
class _TeacherMarksState extends State<TeacherMarksScreen> {
  List<Map<String,dynamic>> students=[];
  final internal=<int,TextEditingController>{};
  final external=<int,TextEditingController>{};
  bool loading=true,saving=false;
  int get scheduleId=>int.tryParse(widget.schedule['id'].toString())??0;
  @override void initState(){super.initState();load();}
  @override void dispose(){for(final c in [...internal.values,...external.values])c.dispose();super.dispose();}
  Future<void> load() async {
    try {
      students=await ApiService.instance.list('/marks/exam-schedule/'+scheduleId.toString()+'/eligible-students');
      final existing=await ApiService.instance.list('/marks/exam-schedule/'+scheduleId.toString());
      final byStudent={for(final x in existing) x['studentId'].toString():x};
      for(final x in students){final id=int.tryParse(x['studentId'].toString());if(id==null)continue;final old=byStudent[id.toString()];internal[id]=TextEditingController(text:old?['internalMarks']?.toString()??'');external[id]=TextEditingController(text:old?['externalMarks']?.toString()??'');}
    } catch(e){if(mounted)snack(context,cleanError(e));}
    finally{if(mounted)setState(()=>loading=false);}
  }
  Future<void> save() async {
    setState(()=>saving=true);
    try {
      for(final x in students){final id=int.tryParse(x['studentId'].toString());if(id==null)continue;final im=int.tryParse(internal[id]?.text.trim()??'');final em=int.tryParse(external[id]?.text.trim()??'');if(im==null&&em==null)continue;await ApiService.instance.mapPost('/marks',{'examScheduleId':scheduleId,'studentId':id,'internalMarks':im,'externalMarks':em});}
      if(mounted)snack(context,'Marks saved successfully.');
    } catch(e){if(mounted)snack(context,cleanError(e));}
    finally{if(mounted)setState(()=>saving=false);}
  }
  @override Widget build(BuildContext context)=>Scaffold(
    appBar:AppBar(title:const Text('Enter Marks')),
    body:loading?const Center(child:CircularProgressIndicator()):ListView(padding:const EdgeInsets.fromLTRB(16,16,16,100),children:[
      Text((widget.schedule['examName']??'Exam').toString()+' · '+(widget.schedule['subjectName']??'Subject').toString(),style:const TextStyle(fontSize:20,fontWeight:FontWeight.w900)),
      const SizedBox(height:16),
      ...students.map((x){final id=int.tryParse(x['studentId'].toString());return Card(child:Padding(padding:const EdgeInsets.all(14),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text((x['studentName']??'Student').toString(),style:const TextStyle(fontWeight:FontWeight.w800)),const SizedBox(height:10),Row(children:[Expanded(child:TextField(controller:id==null?null:internal[id],keyboardType:TextInputType.number,decoration:const InputDecoration(labelText:'Internal'))),const SizedBox(width:10),Expanded(child:TextField(controller:id==null?null:external[id],keyboardType:TextInputType.number,decoration:const InputDecoration(labelText:'External')))])])));}),
      const SizedBox(height:16),
      FilledButton.icon(onPressed:saving?null:save,icon:const Icon(Icons.save_outlined),label:Text(saving?'Saving…':'Save marks'))
    ])
  );
}
