import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { getTeacherDashboardSummary, getUsername, TeacherDashboardSummary } from '../api';
import { colors, radius, spacing } from '../theme';

export function TeacherDashboardScreen({ navigation }: { navigation: any }) {
  const [name, setName] = useState('Teacher');
  const [data, setData] = useState<TeacherDashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    try {
      refresh ? setRefreshing(true) : setLoading(true);
      setError('');
      const [username, summary] = await Promise.all([getUsername(), getTeacherDashboardSummary()]);
      if (username) setName(username);
      setData(summary);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to load teacher dashboard.');
    } finally { setLoading(false); setRefreshing(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const modules = [['Classes','Open assigned classes'],['Attendance','Manage class attendance'],['Assignments','Review coursework'],['Timetable','View your schedule'],['Profile','View your account']];

  return <ScrollView style={s.page} contentContainerStyle={s.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => load(true)} />}>
    <Text style={s.eyebrow}>TEACHER PORTAL</Text><Text style={s.title}>Welcome, {data?.teacherName || name}.</Text>
    <Text style={s.muted}>Your teaching workspace, connected to the live EduSphere ERP backend.</Text>
    {error ? <View style={s.error}><Text style={s.errorText}>{error}</Text><Pressable onPress={() => load()}><Text style={s.retry}>Retry</Text></Pressable></View> : null}
    {loading && !data ? <View style={s.loading}><ActivityIndicator size="large" color={colors.primary}/><Text style={s.muted}>Loading teaching data…</Text></View> : null}
    {data ? <>
      <View style={s.stats}>
        <Stat value={data.totalSubjects} label="Subjects"/><Stat value={data.totalStudents} label="Students"/><Stat value={data.pendingReviewCount} label="Pending review"/><Stat value={data.attendancePendingToday} label="Attendance due"/>
      </View>
      <View style={s.hero}><Text style={s.heroLabel}>TODAY</Text><Text style={s.heroTitle}>{data.upcomingClassesCount} upcoming class{data.upcomingClassesCount === 1 ? '' : 'es'}</Text><Text style={s.heroText}>{data.schedulePlaceholder ? 'Your schedule is not available yet.' : `${data.todaysSchedule?.length ?? 0} scheduled slot${(data.todaysSchedule?.length ?? 0) === 1 ? '' : 's'} loaded.`}</Text></View>
      {data.recentAssignments?.length ? <View style={s.card}><Text style={s.sectionTitle}>Recent assignments</Text>{data.recentAssignments.slice(0,4).map((a:any)=><View key={a.assignmentId} style={s.assignment}><View style={s.body}><Text style={s.cardTitle}>{a.title}</Text><Text style={s.cardText}>{a.subjectName} · Due {new Date(a.dueDate).toLocaleDateString()}</Text></View><Text style={s.count}>{a.pendingReviewCount ?? 0}</Text></View>)}</View> : null}
    </> : null}
    {modules.map(([title,desc])=><Pressable key={title} style={s.card} onPress={()=>navigation.navigate(title)}><View style={s.icon}><Text style={s.iconText}>{title[0]}</Text></View><View style={s.body}><Text style={s.cardTitle}>{title}</Text><Text style={s.cardText}>{desc}</Text></View><Text style={s.arrow}>›</Text></Pressable>)}
  </ScrollView>;
}
function Stat({ value, label }: { value: number; label: string }) { return <View style={s.stat}><Text style={s.statValue}>{value ?? 0}</Text><Text style={s.statLabel}>{label}</Text></View>; }
const s=StyleSheet.create({page:{flex:1,backgroundColor:colors.background},content:{padding:spacing.lg,paddingTop:spacing.xl,paddingBottom:spacing.xxl},eyebrow:{color:colors.primary,fontSize:12,fontWeight:'800',letterSpacing:1.5},title:{color:colors.text,fontSize:30,fontWeight:'800',marginTop:spacing.sm},muted:{color:colors.muted,fontSize:15,lineHeight:23,marginTop:spacing.sm},error:{backgroundColor:'#ffebee',borderRadius:14,padding:12,marginTop:spacing.md},errorText:{color:'#b71c1c'},retry:{color:colors.primary,fontWeight:'800',marginTop:8},loading:{alignItems:'center',padding:spacing.xl},stats:{flexDirection:'row',flexWrap:'wrap',gap:spacing.sm,marginTop:spacing.lg},stat:{backgroundColor:colors.card,borderRadius:radius.card,padding:spacing.md,width:'48%',elevation:1},statValue:{fontSize:24,fontWeight:'800',color:colors.primary},statLabel:{fontSize:12,color:colors.muted,marginTop:4},hero:{backgroundColor:colors.primary,borderRadius:radius.card,padding:spacing.lg,marginTop:spacing.lg,marginBottom:spacing.md},heroLabel:{color:'#d9f2dc',fontSize:11,fontWeight:'800',letterSpacing:1.2},heroTitle:{color:colors.card,fontSize:24,fontWeight:'800',marginTop:spacing.xs},heroText:{color:'#e8f5e9',fontSize:14,lineHeight:21,marginTop:spacing.xs},card:{backgroundColor:colors.card,borderRadius:radius.card,padding:spacing.md,marginTop:spacing.sm,elevation:2},sectionTitle:{fontSize:18,fontWeight:'800',color:colors.text},assignment:{borderTopWidth:1,borderTopColor:'#eef0eb',paddingTop:12,marginTop:12,flexDirection:'row',alignItems:'center'},icon:{width:48,height:48,borderRadius:16,backgroundColor:colors.lightGreen,alignItems:'center',justifyContent:'center'},iconText:{color:colors.primary,fontSize:19,fontWeight:'800'},body:{flex:1,marginLeft:spacing.md},cardTitle:{color:colors.text,fontSize:16,fontWeight:'800'},cardText:{color:colors.muted,fontSize:13,marginTop:3},count:{minWidth:28,textAlign:'center',color:colors.primary,fontWeight:'800'},arrow:{color:colors.primary,fontSize:28,marginLeft:spacing.sm}});
