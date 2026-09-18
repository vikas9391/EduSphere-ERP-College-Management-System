part of 'main.dart';

class AdminUsersScreen extends StatefulWidget {
  const AdminUsersScreen({super.key});
  @override State<AdminUsersScreen> createState()=>_AdminUsersState();
}
class _AdminUsersState extends State<AdminUsersScreen> {
  List<Map<String,dynamic>> users=[]; List<Map<String,dynamic>> roles=[]; bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load() async {
    try{final r=await Future.wait([ApiService.instance.request('/users'),ApiService.instance.list('/roles')]);final raw=r[0];if(raw is Map&&raw['content'] is List)users=(raw['content'] as List).whereType<Map>().map((x)=>Map<String,dynamic>.from(x)).toList();else if(raw is List)users=raw.whereType<Map>().map((x)=>Map<String,dynamic>.from(x)).toList();roles=r[1] as List<Map<String,dynamic>>;}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  Future<void> assignRole(Map<String,dynamic> user) async {
    if(roles.isEmpty||user['id']==null)return;
    final selected=await showDialog<int>(context:context,builder:(c){int? value=int.tryParse('${user['roleId']}');return StatefulBuilder(builder:(c,set)=>AlertDialog(title:const Text('Assign role'),content:DropdownButtonFormField<int>(value:value,items:roles.map((r)=>DropdownMenuItem<int>(value:int.tryParse('${r['id']}'),child:Text('${r['name']??r['roleName']??'Role'}'))).toList(),onChanged:(v)=>set(()=>value=v),decoration:const InputDecoration(labelText:'Role')),actions:[TextButton(onPressed:()=>Navigator.pop(c),child:const Text('Cancel')),FilledButton(onPressed:()=>Navigator.pop(c,value),child:const Text('Save'))]));});
    if(selected==null)return;
    try{await ApiService.instance.mapPut('/users/${user['id']}/role',{'roleId':selected});if(mounted)snack(context,'Role updated.');await load();}catch(e){if(mounted)snack(context,cleanError(e));}
  }
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:const Text('User Management'),actions:[IconButton(onPressed:load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:users.isEmpty?const ListView(children:[SizedBox(height:180),Center(child:Text('No users found.'))]):ListView.builder(padding:const EdgeInsets.all(16),itemCount:users.length,itemBuilder:(c,i){final u=users[i];return Card(child:ListTile(title:Text('${u['firstName']??''} ${u['lastName']??''}'.trim(),style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text('${u['email']??'—'} · ${u['roleName']??'—'}'),trailing:IconButton(onPressed:()=>assignRole(u),icon:const Icon(Icons.manage_accounts_outlined)));})),floatingActionButton:roles.isEmpty?null:FloatingActionButton.extended(onPressed:()=>showDialog(context:context,builder:(_)=>const AlertDialog(content:Text('Use the web admin panel to create users; mobile role assignment is enabled here.'))),icon:const Icon(Icons.person_add_outlined),label:const Text('Create user')));
}
