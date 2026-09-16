import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { getUsername } from '../api';
import { colors, radius, spacing } from '../theme';

export function TeacherDashboardScreen({ navigation }: { navigation: any }) {
  const [name, setName] = useState('Teacher');
  useEffect(() => { getUsername().then((value) => value && setName(value)); }, []);
  return <ScrollView style={s.page} contentContainerStyle={s.content}>
    <Text style={s.eyebrow}>TEACHER PORTAL</Text><Text style={s.title}>Welcome, {name}.</Text>
    <Text style={s.muted}>Manage your assigned classes using the same EduSphere ERP backend.</Text>
    <View style={s.hero}><Text style={s.heroLabel}>TEACHING WORKSPACE</Text><Text style={s.heroTitle}>Your academic day.</Text><Text style={s.heroText}>Classes, attendance, assignments and timetable in one place.</Text></View>
    {['Classes','Attendance','Assignments','Timetable','Profile'].map((item) => <Pressable key={item} style={s.card} onPress={() => navigation.navigate(item)}><View style={s.icon}><Text style={s.iconText}>{item[0]}</Text></View><View style={s.body}><Text style={s.cardTitle}>{item}</Text><Text style={s.cardText}>Open {item.toLowerCase()}</Text></View><Text style={s.arrow}>›</Text></Pressable>)}
  </ScrollView>;
}
const s=StyleSheet.create({page:{flex:1,backgroundColor:colors.background},content:{padding:spacing.lg,paddingTop:spacing.xl,paddingBottom:spacing.xxl},eyebrow:{color:colors.primary,fontSize:12,fontWeight:'800',letterSpacing:1.5},title:{color:colors.text,fontSize:30,fontWeight:'800',marginTop:spacing.sm},muted:{color:colors.muted,fontSize:15,lineHeight:23,marginTop:spacing.sm},hero:{backgroundColor:colors.primary,borderRadius:radius.card,padding:spacing.lg,marginTop:spacing.xl,marginBottom:spacing.md},heroLabel:{color:'#d9f2dc',fontSize:11,fontWeight:'800',letterSpacing:1.2},heroTitle:{color:colors.card,fontSize:24,fontWeight:'800',marginTop:spacing.xs},heroText:{color:'#e8f5e9',fontSize:14,lineHeight:21,marginTop:spacing.xs},card:{backgroundColor:colors.card,borderRadius:radius.card,padding:spacing.md,marginTop:spacing.sm,flexDirection:'row',alignItems:'center',elevation:2},icon:{width:48,height:48,borderRadius:16,backgroundColor:colors.lightGreen,alignItems:'center',justifyContent:'center'},iconText:{color:colors.primary,fontSize:19,fontWeight:'800'},body:{flex:1,marginLeft:spacing.md},cardTitle:{color:colors.text,fontSize:16,fontWeight:'800'},cardText:{color:colors.muted,fontSize:13,marginTop:3},arrow:{color:colors.primary,fontSize:28,marginLeft:spacing.sm}});
