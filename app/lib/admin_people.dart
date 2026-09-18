part of 'main.dart';

class AdminPersonFormScreen extends StatefulWidget{
  final String type; final Map<String,dynamic>? initial;
  const AdminPersonFormScreen({super.key,required this.type,this.initial});
  @override State<AdminPersonFormScreen> createState()=>_AdminPersonFormState();
}
class _AdminPersonFormState extends State<AdminPersonFormScreen>{
  final form=GlobalKey<FormState>(); final fields=<String,TextEditingController>{}; bool saving=false;
  bool get student=>widget.type=='Student';
  @override void initState(){super.initState();final names=student?['admissionNo','rollNumber','firstName','lastName','email','password','phone','gender','city','state','pincode','fatherName','motherName']:['employeeId','firstName','lastName','email','password','phone','gender','qualification','specialization','experience','joiningDate'];for(final n in names)fields[n]=TextEditingController(text:'${widget.initial?[n]??''}');}
  @override void dispose(){for(final x in fields.values)x.dispose();super.dispose();}
  Future<void> save()async{if(!form.currentState!.validate())return;setState(()=>saving=true);final p=<String,dynamic>{};for(final e in fields.entries){final v=e.value.text.trim();if(v.isNotEmpty)p[e.key]=e.key=='experience'?int.tryParse(v):v;}try{final id=widget.initial?['id'];final path=student?'students':'teachers';if(id!=null)await ApiService.instance.mapPut('/$path/$id',p);else await ApiService.instance.mapPost('/$path',p);if(mounted){snack(context,widget.type+' saved.');Navigator.pop(context,true);}}catch(e){if(mounted)snack(context,cleanError(e));}finally{if(mounted)setState(()=>saving=false);}}
  @override Widget build(BuildContext context)=>Scaffold(appBar:AppBar(title:Text((widget.initial==null?'Add ':'Edit ')+widget.type)),body:Form(key:form,child:ListView(padding:const EdgeInsets.all(16),children:[for(final e in fields.entries)Padding(padding:const EdgeInsets.only(bottom:10),child:TextFormField(controller:e.value,obscureText:e.key=='password',decoration:InputDecoration(labelText:e.key),validator:(v){if(['admissionNo','employeeId','firstName','email'].contains(e.key)&&(v??'').trim().isEmpty)return 'Required';if(e.key=='password'&&widget.initial==null&&(v??'').length<8)return 'Minimum 8 characters';return null;}}),FilledButton.icon(onPressed:saving?null:save,icon:const Icon(Icons.save_outlined),label:Text(saving?'Saving...':'Save'))]));}
}
