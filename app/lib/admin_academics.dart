part of 'main.dart';

class AdminResourceScreen extends StatefulWidget {
  final String title;
  final String endpoint;
  final List<String> fields;
  const AdminResourceScreen({super.key,required this.title,required this.endpoint,required this.fields});
  @override State<AdminResourceScreen> createState()=>_AdminResourceState();
}
class _AdminResourceState extends State<AdminResourceScreen>{
  List<Map<String,dynamic>> rows=[];bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load()async{try{final raw=await ApiService.instance.request(widget.endpoint);final value=raw is Map&&raw['content'] is List?raw['content']:raw is List?raw:[];rows=value.whereType<Map>().map((x)=>Map<String,dynamic>.from(x)).toList();}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:Text(widget.title),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:rows.isEmpty?ListView(children:[const SizedBox(height:180),Center(child:Text('No records found.'))]):ListView.separated(padding:const EdgeInsets.all(16),itemCount:rows.length,separatorBuilder:(_,__)=>const SizedBox(height:8),itemBuilder:(c,i){final r=rows[i];final heading=r['name']??r['courseName']??r['subjectName']??r['firstName']??r['className']??'Record #${r['id']??i+1}';final details=widget.fields.map((f)=>r[f]).where((v)=>v!=null&&'$v'.trim().isNotEmpty).map((v)=>'$v').join(' · ');return Card(child:ListTile(title:Text('$heading',style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:details.isEmpty?null:Text(details)));})));
}

class AdminAnnouncementsScreen extends StatefulWidget{const AdminAnnouncementsScreen({super.key});@override State<AdminAnnouncementsScreen> createState()=>_AdminAnnouncementsState();}
class _AdminAnnouncementsState extends State<AdminAnnouncementsScreen>{
  List<Map<String,dynamic>> items=[];bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load()async{try{final raw=await ApiService.instance.request('/announcements');final v=raw is List?raw:[];items=v.whereType<Map>().map((x)=>Map<String,dynamic>.from(x)).toList();}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  Future<void> compose()async{final title=TextEditingController(),message=TextEditingController();String audience='ALL';final ok=await showDialog<bool>(context:context,builder:(c)=>StatefulBuilder(builder:(c,set)=>AlertDialog(title:const Text('New announcement'),content:SingleChildScrollView(child:Column(children:[TextField(controller:title,decoration:const InputDecoration(labelText:'Title')),TextField(controller:message,maxLines:5,decoration:const InputDecoration(labelText:'Message')),DropdownButtonFormField<String>(value:audience,items:const[DropdownMenuItem(value:'ALL',child:Text('Everyone')),DropdownMenuItem(value:'STUDENTS',child:Text('Students')),DropdownMenuItem(value:'TEACHERS',child:Text('Teachers'))],onChanged:(v)=>set(()=>audience=v??'ALL'),decoration:const InputDecoration(labelText:'Audience'))])),actions:[TextButton(onPressed:()=>Navigator.pop(c,false),child:const Text('Cancel')),FilledButton(onPressed:()=>Navigator.pop(c,title.text.trim().isNotEmpty&&message.text.trim().isNotEmpty),child:const Text('Send'))]));if(ok==true){try{await ApiService.instance.mapPost('/announcements',{'title':title.text.trim(),'message':message.text.trim(),'audienceType':audience});if(mounted)snack(context,'Announcement sent.');await load();}catch(e){if(mounted)snack(context,cleanError(e));}}title.dispose();message.dispose();}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('Announcements'),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:items.isEmpty?ListView(children:[const SizedBox(height:180),Center(child:Text('No announcements found.'))]):ListView.builder(padding:const EdgeInsets.all(16),itemCount:items.length,itemBuilder:(c,i){final x=items[i];return Card(child:ListTile(title:Text('${x['title']??'Announcement'}',style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text('${x['message']??''}'));})),floatingActionButton:FloatingActionButton.extended(onPressed:compose,icon:const Icon(Icons.campaign_outlined),label:const Text('New')));}
