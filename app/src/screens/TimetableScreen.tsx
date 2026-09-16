import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { colors, radius, spacing } from '../theme';
export function TimetableScreen(){return <ScrollView style={s.page} contentContainerStyle={s.content}><Text style={s.title}>My Timetable</Text><Text style={s.muted}>Your weekly schedule is derived from ClassEnrollment → ClassSubject → TimetableEntry.</Text><View style={s.card}><Text style={s.cardTitle}>Weekly schedule</Text><Text style={s.muted}>Ready for live timetable entries from the existing backend.</Text></View></ScrollView>}
const s=StyleSheet.create({page:{flex:1,backgroundColor:colors.background},content:{padding:spacing.lg,paddingTop:spacing.xl},title:{fontSize:28,fontWeight:'800',color:colors.text},muted:{fontSize:15,lineHeight:23,color:colors.muted,marginTop:spacing.sm},card:{backgroundColor:colors.card,borderRadius:radius.card,padding:spacing.lg,marginTop:spacing.xl},cardTitle:{fontSize:18,fontWeight:'800',color:colors.text}});
