import React, { useCallback, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { getMyResults, MyOverallResult } from '../api';
import { useFocusEffect } from '@react-navigation/native';

export function ResultsScreen() {
  const [result, setResult] = useState<MyOverallResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    try {
      refresh ? setRefreshing(true) : setLoading(true);
      setError('');
      setResult(await getMyResults());
    } catch (e: any) {
      setError(e?.message ?? 'Unable to load results.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  if (loading && !result) return <View style={styles.center}><ActivityIndicator size="large" color="#2e7d32"/><Text style={styles.muted}>Loading results…</Text></View>;

  return <ScrollView style={styles.page} contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => load(true)} />}>
    <Text style={styles.title}>Results</Text>
    {error ? <View style={styles.error}><Text style={styles.errorText}>{error}</Text></View> : null}
    {result ? <>
      <View style={styles.summary}>
        <View><Text style={styles.label}>CGPA</Text><Text style={styles.cgpa}>{Number(result.cgpa ?? 0).toFixed(2)}</Text></View>
        <View style={styles.resultBadge}><Text style={styles.badgeText}>{result.overallResult || '—'}</Text></View>
      </View>
      {(result.semesterResults ?? []).map((semester) => <View key={`${semester.semester}-${semester.academicYear}`} style={styles.card}>
        <View style={styles.row}><Text style={styles.semester}>Semester {semester.semester}</Text><Text style={styles.sgpa}>SGPA {Number(semester.sgpa ?? 0).toFixed(2)}</Text></View>
        <Text style={styles.muted}>{semester.academicYear} · {semester.totalCredits ?? 0} credits · {semester.result || '—'}</Text>
        {(semester.subjects ?? []).map((subject) => <View key={subject.subjectId} style={styles.subject}>
          <View style={styles.subjectMain}><Text style={styles.subjectName}>{subject.subjectName}</Text><Text style={styles.code}>{subject.subjectCode}</Text></View>
          <View style={styles.marks}><Text style={styles.total}>{subject.totalMarks}/{subject.maxMarks}</Text><Text style={styles.grade}>{subject.grade || '—'} · {Number(subject.gradePoint ?? 0).toFixed(1)}</Text></View>
        </View>)}
      </View>)}
      {!result.semesterResults?.length ? <Text style={styles.muted}>No semester results are available yet.</Text> : null}
    </> : null}
  </ScrollView>;
}

const styles = StyleSheet.create({
  page:{flex:1,backgroundColor:'#f8f8f2'}, content:{padding:20,paddingBottom:40}, center:{flex:1,alignItems:'center',justifyContent:'center',backgroundColor:'#f8f8f2',gap:10},
  title:{fontSize:30,fontWeight:'800',color:'#1f2937',marginBottom:16}, muted:{color:'#6b7280',fontSize:13}, error:{backgroundColor:'#ffebee',padding:12,borderRadius:12,marginBottom:14}, errorText:{color:'#b71c1c'},
  summary:{backgroundColor:'#e8f5e9',borderRadius:18,padding:18,flexDirection:'row',justifyContent:'space-between',alignItems:'center',marginBottom:16}, label:{color:'#4b5563',fontSize:13}, cgpa:{fontSize:32,fontWeight:'800',color:'#2e7d32',marginTop:2}, resultBadge:{backgroundColor:'#2e7d32',paddingHorizontal:12,paddingVertical:8,borderRadius:20}, badgeText:{color:'#fff',fontWeight:'700'},
  card:{backgroundColor:'#fff',borderRadius:18,padding:16,marginBottom:14}, row:{flexDirection:'row',justifyContent:'space-between',alignItems:'center'}, semester:{fontSize:19,fontWeight:'800',color:'#1f2937'}, sgpa:{fontWeight:'800',color:'#2e7d32'}, subject:{borderTopWidth:1,borderTopColor:'#eef0eb',paddingTop:12,marginTop:12,flexDirection:'row',justifyContent:'space-between',gap:12}, subjectMain:{flex:1}, subjectName:{fontSize:15,fontWeight:'700',color:'#1f2937'}, code:{fontSize:12,color:'#6b7280',marginTop:3}, marks:{alignItems:'flex-end'}, total:{fontWeight:'800',color:'#1f2937'}, grade:{fontSize:12,color:'#2e7d32',marginTop:3}
});
