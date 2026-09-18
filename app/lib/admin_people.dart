part of 'main.dart';

class AdminPeopleListScreen extends StatefulWidget{
  final String type; const AdminPeopleListScreen({super.key,required this.type});
  @override State<AdminPeopleListScreen> createState()=>_AdminPeopleListState();
}
class _AdminPeopleListState extends State<AdminPeopleListScreen>{
  List<Map<String,dynamic>> rows=[];bool loading=true;
  String get endpoint=>widget.type=='Student'?'/students':'/teachers';
  @override void initState(){super.initState();load();}
  Future<void> load()async{try{final raw=await ApiService.instance.request(endpoint);final v=raw is Map&&raw['content'] is List?raw['content']:raw is List?raw:[];rows=v.whereType<Map>().map((x)=>Map<String,dynamic>.from(x)).toList();}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  Future<void> add()async{final ok=await Navigator.push<bool>(context,MaterialPageRoute(builder:(_)=>AdminPersonFormScreen(type:widget.type)));if(ok==true)load();}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:Text(widget.type+' Management'),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:rows.isEmpty?ListView(children:[const SizedBox(height:180),Center(child:Text('No records found.'))]):ListView.builder(padding:const EdgeInsets.all(16),itemCount:rows.length,itemBuilder:(c,i){final r=rows[i];final name=((r['firstName']??'')+' '+(r['lastName']??'')).trim();return Card(child:ListTile(title:Text(name.isEmpty?'Record #'+(r['id']??'').toString():name,style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text((widget.type=='Student'?(r['admissionNo']??''):(r['employeeId']??''))+' · '+(r['email']??'')),trailing:const Icon(Icons.chevron_right)));})),floatingActionButton:FloatingActionButton.extended(onPressed:add,icon:const Icon(Icons.add),label:Text('Add '+widget.type)));}
}
