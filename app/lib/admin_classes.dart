part of 'main.dart';

class AdminClassDetailScreen extends StatefulWidget{
  final Map<String,dynamic> item; const AdminClassDetailScreen({super.key,required this.item});
  @override State<AdminClassDetailScreen> createState()=>_AdminClassDetailState();
}
class _AdminClassDetailState extends State<AdminClassDetailScreen>{
  List<Map<String,dynamic>> students=[];List<Map<String,dynamic>> subjects=[];bool loading=true;
  int get id=>int.parse('${widget.item['id']}');
  @override void initState(){super.initState();load();}
  Future<void> load()async{try{final r=await Future.wait([ApiService.instance.list('/classes/$id/students'),ApiService.instance.list('/classes/$id/subjects')]);students=r[0];subjects=r[1];}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  Future<void> addStudents()async{final controller=TextEditingController();final ok=await showDialog<bool>(context:context,builder:(c)=>AlertDialog(title:const Text('Add students'),content:TextField(controller:controller,keyboardType:TextInputType.number,decoration:const InputDecoration(labelText:'Student IDs',hintText:'12, 15, 20')),actions:[TextButton(onPressed:()=>Navigator.pop(c,false),child:const Text('Cancel')),FilledButton(onPressed:()=>Navigator.pop(c,controller.text.trim().isNotEmpty),child:const Text('Add'))]));if(ok==true){try{final ids=controller.text.split(',').map((x)=>int.tryParse(x.trim())).whereType<int>().toList();await ApiService.instance.mapPost('/classes/$id/students',{'studentIds':ids});if(mounted)snack(context,'Students added.');load();}catch(e){if(mounted)snack(context,cleanError(e));}}controller.dispose();}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:Text('${widget.item['name']??'Class'}')),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:ListView(padding:const EdgeInsets.all(16),children:[Text('${widget.item['academicYear']??''} · Semester ${widget.item['semester']??''}',style:Theme.of(context).textTheme.titleMedium),const SizedBox(height:20),Row(mainAxisAlignment:MainAxisAlignment.spaceBetween,children:[Text('Students (${students.length})',style:const TextStyle(fontSize:18,fontWeight:FontWeight.bold)),IconButton(onPressed:addStudents,icon:const Icon(Icons.person_add_alt_1))]),...students.map((s)=>Card(child:ListTile(title:Text('${s['studentName']??'Student'}'),subtitle:Text('${s['admissionNo']??''}')))),const SizedBox(height:20),Text('Subjects (${subjects.length})',style:const TextStyle(fontSize:18,fontWeight:FontWeight.bold)),...subjects.map((s)=>Card(child:ListTile(title:Text('${s['subjectName']??s['subjectCode']??'Subject'}'),subtitle:Text('${s['teacherName']??'No teacher'} · ${s['enrollmentMode']??''}'))))])));
}
