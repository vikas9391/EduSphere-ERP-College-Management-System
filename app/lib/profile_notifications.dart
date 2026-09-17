part of 'main.dart';

class ProfileScreen extends StatefulWidget {
  final String role;
  const ProfileScreen({super.key, required this.role});
  @override State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  Map<String, dynamic> profile = {};
  bool loading = true, saving = false, changingPassword = false;
  final phone = TextEditingController(), address = TextEditingController(), city = TextEditingController(), state = TextEditingController(), pincode = TextEditingController(), parentPhone = TextEditingController(), parentEmail = TextEditingController(), bloodGroup = TextEditingController();

  @override void initState() { super.initState(); load(); }
  @override void dispose() { for (final c in [phone,address,city,state,pincode,parentPhone,parentEmail,bloodGroup]) { c.dispose(); } super.dispose(); }
  void setc(TextEditingController c, dynamic v) => c.text = v?.toString() ?? '';

  Future<void> load() async {
    setState(() => loading = true);
    try {
      final p = await ApiService.instance.map('/student/profile'); profile = p;
      setc(phone,p['phone']); setc(address,p['address']); setc(city,p['city']); setc(state,p['state']); setc(pincode,p['pincode']); setc(parentPhone,p['parentPhone']); setc(parentEmail,p['parentEmail']); setc(bloodGroup,p['bloodGroup']);
    } catch (e) { if (mounted) snack(context, cleanError(e)); }
    finally { if (mounted) setState(() => loading = false); }
  }

  Future<void> save() async {
    setState(() => saving = true);
    try {
      profile = await ApiService.instance.mapPut('/student/profile', {'phone':phone.text.trim(),'address':address.text.trim(),'city':city.text.trim(),'state':state.text.trim(),'pincode':pincode.text.trim(),'parentPhone':parentPhone.text.trim(),'parentEmail':parentEmail.text.trim(),'bloodGroup':bloodGroup.text.trim()});
      if (mounted) snack(context,'Profile updated successfully');
    } catch (e) { if (mounted) snack(context,cleanError(e)); }
    finally { if (mounted) setState(() => saving = false); }
  }

  Future<void> passwordDialog() async {
    final oldC=TextEditingController(), newC=TextEditingController(), confirmC=TextEditingController();
    final ok = await showDialog<bool>(context:context,builder:(d)=>AlertDialog(title:const Text('Change password'),content:Column(mainAxisSize:MainAxisSize.min,children:[TextField(controller:oldC,obscureText:true,decoration:const InputDecoration(labelText:'Current password')),TextField(controller:newC,obscureText:true,decoration:const InputDecoration(labelText:'New password')),TextField(controller:confirmC,obscureText:true,decoration:const InputDecoration(labelText:'Confirm new password'))]),actions:[TextButton(onPressed:()=>Navigator.pop(d,false),child:const Text('Cancel')),FilledButton(onPressed:() async { if(newC.text.length<8||newC.text!=confirmC.text){snack(d,'Use 8+ characters and matching passwords.');return;} try {await ApiService.instance.mapPut('/student/change-password',{'oldPassword':oldC.text,'newPassword':newC.text});if(d.mounted)Navigator.pop(d,true);}catch(e){if(d.mounted)snack(d,cleanError(e));}},child:const Text('Update'))]));
    oldC.dispose(); newC.dispose(); confirmC.dispose(); if(ok==true&&mounted)snack(context,'Password changed successfully');
  }

  @override Widget build(BuildContext context) {
    if (!widget.role.toUpperCase().contains('STUDENT')) return Scaffold(appBar:AppBar(title:const Text('Profile')),body:const Center(child:Text('Profile management is currently available for students.')));
    return Scaffold(appBar:AppBar(title:const Text('My Profile')),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:ListView(padding:const EdgeInsets.all(20),children:[Card(child:Padding(padding:const EdgeInsets.all(20),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text('${profile['firstName']??''} ${profile['lastName']??''}'.trim(),style:const TextStyle(fontSize:26,fontWeight:FontWeight.w900)),Text('${profile['admissionNo']??'—'} · ${profile['email']??'—'}',style:const TextStyle(color:Colors.black54)),const SizedBox(height:12),Text('${profile['courseName']??'Course'} · ${profile['department']??'Department'}')])),),_section('Contact',[f('Phone',phone),f('Address',address),f('City',city),f('State',state),f('Pincode',pincode)]),_section('Parent',[f('Parent phone',parentPhone),f('Parent email',parentEmail)]),_section('Personal',[f('Blood group',bloodGroup)]),SizedBox(height:52,child:FilledButton.icon(onPressed:saving?null:save,icon:const Icon(Icons.save_outlined),label:Text(saving?'Saving…':'Save profile'))),const SizedBox(height:10),OutlinedButton.icon(onPressed:changingPassword?null:()async{setState(()=>changingPassword=true);await passwordDialog();if(mounted)setState(()=>changingPassword=false);},icon:const Icon(Icons.lock_outline),label:Text('Change password')),const SizedBox(height:24)])));
  }
  Widget _section(String title,List<Widget> children)=>Padding(padding:const EdgeInsets.only(top:12),child:Card(child:Padding(padding:const EdgeInsets.all(18),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(title,style:const TextStyle(fontSize:18,fontWeight:FontWeight.w900)),const SizedBox(height:12),...children]))));
  Widget f(String label,TextEditingController c)=>Padding(padding:const EdgeInsets.only(bottom:10),child:TextField(controller:c,decoration:InputDecoration(labelText:label)));
}

class NotificationsScreen extends StatefulWidget { const NotificationsScreen({super.key}); @override State<NotificationsScreen> createState()=>_NotificationsScreenState(); }
class _NotificationsScreenState extends State<NotificationsScreen> {
  List<Map<String,dynamic>> items=[]; bool loading=true;
  @override void initState(){super.initState();load();}
  Future<void> load() async {setState(()=>loading=true);try{items=await ApiService.instance.list('/announcements');}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>loading=false);}}
  Future<void> read(Map<String,dynamic> x) async {final id=x['id'];if(id==null||x['read']==true)return;try{await ApiService.instance.request('/announcements/$id/read',method:'POST');setState(()=>x['read']=true);}catch(e){if(mounted)snack(context,cleanError(e));}}
  Future<void> readAll() async {try{await ApiService.instance.request('/announcements/read-all',method:'POST');setState((){for(final x in items){x['read']=true;}});}catch(e){if(mounted)snack(context,cleanError(e));}}
  @override Widget build(BuildContext context){final unread=items.where((x)=>x['read']!=true).length;return Scaffold(appBar:AppBar(title:const Text('Notifications'),actions:[if(unread>0)TextButton(onPressed:readAll,child:const Text('Read all'))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:load,child:items.isEmpty?ListView(children:const[ SizedBox(height:180),Center(child:Text('No notifications'))]):ListView.builder(padding:const EdgeInsets.all(16),itemCount:items.length,itemBuilder:(c,i){final x=items[i],isRead=x['read']==true;return Card(color:isRead?null:const Color(0xFFE8F5E9),child:ListTile(onTap:()=>read(x),leading:CircleAvatar(backgroundColor:isRead?Colors.black12:green,child:Icon(isRead?Icons.notifications_none:Icons.notifications,color:isRead?Colors.black54:Colors.white)),title:Text('${x['title']??'Announcement'}',style:const TextStyle(fontWeight:FontWeight.w800)),subtitle:Text('${x['message']??''}'),trailing:isRead?null:const Icon(Icons.circle,size:10,color:green)));}))));}
}
