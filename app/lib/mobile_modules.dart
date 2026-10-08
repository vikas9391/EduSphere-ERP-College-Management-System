part of 'main.dart';

class StudentAttendanceScreen extends StatefulWidget {
  const StudentAttendanceScreen({super.key});
  @override State<StudentAttendanceScreen> createState() => _StudentAttendanceScreenState();
}
class _StudentAttendanceScreenState extends State<StudentAttendanceScreen> {
  Map<String,dynamic> data={}; bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load() async {
    setState(()=>loading=true);
    try { final v=await ApiService.instance.request('/attendance/me/summary'); if(v is Map)data=Map<String,dynamic>.from(v); }
    catch(e){if(mounted)snack(context,cleanError(e));} finally{if(mounted)setState(()=>loading=false);}
  }
  @override Widget build(BuildContext c)=>Scaffold(backgroundColor:pageBg,appBar:AppBar(title:const Text('My Attendance',style:TextStyle(fontWeight:FontWeight.w800)),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh_rounded))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(color:navy,onRefresh:load,child:ListView(padding:const EdgeInsets.all(20),children:[_summary(),const SizedBox(height:18),const Text('By subject',style:TextStyle(fontSize:19,fontWeight:FontWeight.w800)),const SizedBox(height:10),..._subjects()])));
  Widget _summary(){
    final p=double.tryParse((data['overallAttendancePercentage']??0).toString())??0;
    final a=data['classesAttended']??0; final m=data['classesMissed']??0;
    return Container(padding:const EdgeInsets.all(20),decoration:BoxDecoration(color:lightGreen,borderRadius:BorderRadius.circular(24),border:Border.all(color:border)),child:Row(children:[SizedBox(width:90,height:90,child:Stack(alignment:Alignment.center,children:[CircularProgressIndicator(value:(p/100).clamp(0,1),strokeWidth:8,color:navy,backgroundColor:Colors.white),Text(p.toStringAsFixed(0)+'%',style:const TextStyle(fontWeight:FontWeight.w900,fontSize:20,color:navy))])),const SizedBox(width:18),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[const Text('Overall attendance',style:TextStyle(fontSize:17,fontWeight:FontWeight.w800)),const SizedBox(height:6),Text(a.toString()+' attended · '+m.toString()+' missed',style:const TextStyle(color:muted)),const SizedBox(height:5),const Text('Keep your attendance above the college minimum.',style:TextStyle(color:muted,fontSize:12))]))]));
  }
  List<Widget> _subjects(){
    final raw=data['bySubject'];
    if(raw is! List||raw.isEmpty)return[const _EmptyModule(message:'No subject attendance is available yet.')];
    return raw.whereType<Map>().map((x){final s=Map<String,dynamic>.from(x);final p=double.tryParse((s['attendancePercentage']??0).toString())??0;return Container(margin:const EdgeInsets.only(bottom:10),padding:const EdgeInsets.all(16),decoration:BoxDecoration(color:Colors.white,borderRadius:BorderRadius.circular(20),border:Border.all(color:border)),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Row(children:[Expanded(child:Text((s['subjectName']??'Subject').toString(),style:const TextStyle(fontWeight:FontWeight.w800))),Text(p.toStringAsFixed(0)+'%',style:const TextStyle(fontWeight:FontWeight.w900,color:navy))]),const SizedBox(height:8),ClipRRect(borderRadius:BorderRadius.circular(10),child:LinearProgressIndicator(value:(p/100).clamp(0,1),minHeight:7,backgroundColor:const Color(0xFFE8EEE5),color:green)),const SizedBox(height:7),Text((s['classesAttended']??0).toString()+' attended · '+(s['classesMissed']??0).toString()+' missed · '+(s['totalClasses']??0).toString()+' total',style:const TextStyle(color:muted,fontSize:12))]));}).toList();
  }
}

class StudentTimetableScreen extends StatefulWidget {
  const StudentTimetableScreen({super.key});
  @override State<StudentTimetableScreen> createState()=>_StudentTimetableScreenState();
}
class _StudentTimetableScreenState extends State<StudentTimetableScreen>{
  Map<String,dynamic> schedule={}; bool loading=true; String? day;
  @override void initState(){super.initState();load();}
  Future<void> load()async{setState(()=>loading=true);try{final v=await ApiService.instance.request('/student/timetable');if(v is Map){final m=Map<String,dynamic>.from(v);schedule=m['schedule'] is Map?Map<String,dynamic>.from(m['schedule']):{};day=day??(schedule.isEmpty?null:schedule.keys.first);}}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  @override Widget build(BuildContext c)=>Scaffold(backgroundColor:pageBg,appBar:AppBar(title:const Text('My Timetable',style:TextStyle(fontWeight:FontWeight.w800)),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh_rounded))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(color:navy,onRefresh:load,child:ListView(padding:const EdgeInsets.all(20),children:[if(schedule.isEmpty)const _EmptyModule(message:'Your timetable has not been published yet.'),if(schedule.isNotEmpty)_days(),const SizedBox(height:14),if(day!=null)..._entries(schedule[day] is List?schedule[day] as List:const [])])));
  Widget _days()=>SingleChildScrollView(scrollDirection:Axis.horizontal,child:Row(children:schedule.keys.map((d){final a=d==day;return Padding(padding:const EdgeInsets.only(right:8),child:ChoiceChip(label:Text(d),selected:a,onSelected:(_)=>setState(()=>day=d),selectedColor:lightGreen));}).toList()));
  List<Widget> _entries(List list){if(list.isEmpty)return[const _EmptyModule(message:'No classes scheduled for this day.')];return list.whereType<Map>().map((x){final e=Map<String,dynamic>.from(x);return Container(margin:const EdgeInsets.only(bottom:10),padding:const EdgeInsets.all(16),decoration:BoxDecoration(color:Colors.white,borderRadius:BorderRadius.circular(20),border:Border.all(color:border)),child:Row(children:[Container(width:66,padding:const EdgeInsets.all(9),decoration:BoxDecoration(color:lightGreen,borderRadius:BorderRadius.circular(13)),child:Text((e['startTime']??'').toString(),textAlign:TextAlign.center,style:const TextStyle(color:navy,fontWeight:FontWeight.w800,fontSize:12))),const SizedBox(width:13),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text((e['subjectName']??'Class').toString(),style:const TextStyle(fontSize:16,fontWeight:FontWeight.w800)),const SizedBox(height:4),Text((e['teacherName']??'Faculty').toString(),style:const TextStyle(color:muted,fontSize:12)),Text('Room '+(e['room']??'—').toString(),style:const TextStyle(color:muted,fontSize:12))]))]));}).toList();}
}

class StudentResultsScreen extends StatefulWidget {
  const StudentResultsScreen({super.key});
  @override State<StudentResultsScreen> createState()=>_StudentResultsScreenState();
}
class _StudentResultsScreenState extends State<StudentResultsScreen>{
  Map<String,dynamic> data={};bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load()async{setState(()=>loading=true);try{final v=await ApiService.instance.request('/student/results');if(v is Map)data=Map<String,dynamic>.from(v);}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  @override Widget build(BuildContext c)=>Scaffold(backgroundColor:pageBg,appBar:AppBar(title:const Text('Results',style:TextStyle(fontWeight:FontWeight.w800)),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh_rounded))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(color:navy,onRefresh:load,child:ListView(padding:const EdgeInsets.all(20),children:[_top(),const SizedBox(height:18),..._semesters()])));
  Widget _top()=>Container(padding:const EdgeInsets.all(20),decoration:BoxDecoration(color:lightGreen,borderRadius:BorderRadius.circular(24),border:Border.all(color:border)),child:Row(children:[Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[const Text('Overall CGPA',style:TextStyle(color:muted)),Text((data['cgpa']??'—').toString(),style:const TextStyle(fontSize:34,fontWeight:FontWeight.w900,color:navy)),Text((data['overallResult']??'').toString(),style:const TextStyle(fontWeight:FontWeight.w700,color:navy))])),Column(crossAxisAlignment:CrossAxisAlignment.end,children:[const Text('Credits',style:TextStyle(color:muted)),Text((data['totalCredits']??0).toString(),style:const TextStyle(fontSize:22,fontWeight:FontWeight.w800))])]));
  List<Widget> _semesters(){final raw=data['semesterResults'];if(raw is! List||raw.isEmpty)return[const _EmptyModule(message:'No semester results are available yet.')];return raw.whereType<Map>().map((x){final s=Map<String,dynamic>.from(x);final subjects=s['subjects'] is List?s['subjects'] as List:const [];return Container(margin:const EdgeInsets.only(bottom:12),padding:const EdgeInsets.all(17),decoration:BoxDecoration(color:Colors.white,borderRadius:BorderRadius.circular(22),border:Border.all(color:border)),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Row(children:[Expanded(child:Text('Semester '+(s['semester']??'—').toString()+' · '+(s['academicYear']??'').toString(),style:const TextStyle(fontSize:17,fontWeight:FontWeight.w800))),Text('SGPA '+(s['sgpa']??'—').toString(),style:const TextStyle(color:navy,fontWeight:FontWeight.w900))]),const SizedBox(height:12),...subjects.whereType<Map>().map((r){final m=Map<String,dynamic>.from(r);return Padding(padding:const EdgeInsets.only(bottom:9),child:Row(children:[Expanded(child:Text((m['subjectCode']??'').toString()+' · '+(m['subjectName']??'Subject').toString(),style:const TextStyle(fontWeight:FontWeight.w600))),Text((m['grade']??'—').toString(),style:const TextStyle(fontWeight:FontWeight.w900,color:navy))]));})]));}).toList();}
}

class StudentFeesScreen extends StatefulWidget {
  const StudentFeesScreen({super.key});
  @override State<StudentFeesScreen> createState()=>_StudentFeesScreenState();
}
class _StudentFeesScreenState extends State<StudentFeesScreen>{
  List<Map<String,dynamic>> fees=[];bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load()async{setState(()=>loading=true);try{final v=await ApiService.instance.request('/fees/mine');fees=v is List?v.whereType<Map>().map((e)=>Map<String,dynamic>.from(e)).toList():[];}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  @override Widget build(BuildContext c){final balance=fees.fold<double>(0,(a,f)=>a+(double.tryParse((f['balance']??0).toString())??0));return Scaffold(backgroundColor:pageBg,appBar:AppBar(title:const Text('My Fees',style:TextStyle(fontWeight:FontWeight.w800)),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh_rounded))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(color:navy,onRefresh:load,child:ListView(padding:const EdgeInsets.all(20),children:[Container(padding:const EdgeInsets.all(20),decoration:BoxDecoration(color:lightGreen,borderRadius:BorderRadius.circular(24),border:Border.all(color:border)),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[const Text('Outstanding balance',style:TextStyle(color:muted)),Text('₹'+balance.toStringAsFixed(2),style:const TextStyle(fontSize:30,fontWeight:FontWeight.w900,color:navy))]),),const SizedBox(height:18),if(fees.isEmpty)const _EmptyModule(message:'No fee records are available yet.'),...fees.map(_fee)])));}
  Widget _fee(Map<String,dynamic> f)=>Container(margin:const EdgeInsets.only(bottom:10),padding:const EdgeInsets.all(17),decoration:BoxDecoration(color:Colors.white,borderRadius:BorderRadius.circular(20),border:Border.all(color:border)),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Row(children:[Expanded(child:Text((f['feeName']??'Fee').toString(),style:const TextStyle(fontSize:16,fontWeight:FontWeight.w800))),Text((f['status']??'PENDING').toString(),style:const TextStyle(color:navy,fontWeight:FontWeight.w800,fontSize:11))]),const SizedBox(height:12),Row(children:[_money('Total',f['totalAmount']),_money('Paid',f['amountPaid']),_money('Balance',f['balance'])]),if(f['dueDate']!=null)Padding(padding:const EdgeInsets.only(top:9),child:Text('Due '+f['dueDate'].toString(),style:const TextStyle(color:muted,fontSize:12)))]);
  Widget _money(String l,dynamic v)=>Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(l,style:const TextStyle(color:muted,fontSize:11)),Text('₹'+(double.tryParse(v.toString())??0).toStringAsFixed(0),style:const TextStyle(fontWeight:FontWeight.w800))]));
}

class _EmptyModule extends StatelessWidget{
  final String message;const _EmptyModule({required this.message});
  @override Widget build(BuildContext c)=>Container(padding:const EdgeInsets.all(25),decoration:BoxDecoration(color:Colors.white,borderRadius:BorderRadius.circular(20),border:Border.all(color:border)),child:Column(children:[const Icon(Icons.inbox_outlined,color:muted,size:34),const SizedBox(height:9),Text(message,textAlign:TextAlign.center,style:const TextStyle(color:muted))]));
}
