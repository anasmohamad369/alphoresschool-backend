'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import { coursesData, Course } from '@/data/courses';
import { videosData, Video } from '@/data/videos';
import { documentsData, DocumentItem } from '@/data/documents';
import { blogsData, BlogPost } from '@/data/blogs';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { 
  GraduationCap, 
  LayoutDashboard, 
  Users, 
  BookOpen, 
  Video as VideoIcon, 
  FileText, 
  Newspaper, 
  CreditCard, 
  Settings, 
  Plus, 
  TrendingUp, 
  Search, 
  X, 
  Menu, 
  LogOut, 
  Trash2, 
  Eye, 
  CheckCircle,
  AlertCircle
} from 'lucide-react';

export default function AdminDashboardPage() {
  const { user, logout } = useApp();
  const [activeTab, setActiveTab] = useState<'dashboard' | 'students' | 'courses' | 'videos' | 'documents' | 'blogs' | 'payments' | 'settings' | 'marks'>('dashboard');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Marks & Results Management State
  const [selectedClassFilter, setSelectedClassFilter] = useState<string>('All');
  const [selectedExamFilter, setSelectedExamFilter] = useState<string>('All');
  const [publishBanner, setPublishBanner] = useState<string | null>(null);

  const [marksList, setMarksList] = useState([
    { id: 1, studentId: 'STU-1001', studentName: 'Aarav Sharma', className: 'Class 10-A', examName: 'Mid-Term Examination 2026', subject: 'Mathematics', marksObtained: 92, maxMarks: 100, published: false },
    { id: 2, studentId: 'STU-1001', studentName: 'Aarav Sharma', className: 'Class 10-A', examName: 'Mid-Term Examination 2026', subject: 'Physics', marksObtained: 88, maxMarks: 100, published: false },
    { id: 3, studentId: 'STU-1002', studentName: 'Priya Verma', className: 'Class 10-A', examName: 'Mid-Term Examination 2026', subject: 'Mathematics', marksObtained: 79, maxMarks: 100, published: false },
    { id: 4, studentId: 'STU-1002', studentName: 'Priya Verma', className: 'Class 10-A', examName: 'Mid-Term Examination 2026', subject: 'Physics', marksObtained: 84, maxMarks: 100, published: false },
    { id: 5, studentId: 'STU-1003', studentName: 'Rohan Gupta', className: 'Class 10-B', examName: 'Mid-Term Examination 2026', subject: 'Mathematics', marksObtained: 65, maxMarks: 100, published: false },
    { id: 6, studentId: 'STU-1004', studentName: 'Ananya Reddy', className: 'Class 9-A', examName: 'Unit Test 1', subject: 'Chemistry', marksObtained: 95, maxMarks: 100, published: true },
  ]);

  const [editingMarkId, setEditingMarkId] = useState<number | null>(null);
  const [editMarksVal, setEditMarksVal] = useState<number>(0);
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);

  const handlePublishClassWise = (className: string) => {
    setMarksList(prev => prev.map(m => (className === 'All' || m.className === className) ? { ...m, published: true } : m));
    setPublishBanner(`Successfully published results ${className !== 'All' ? `for ${className}` : 'class-wise'}! Students can now view their report cards.`);
    setTimeout(() => setPublishBanner(null), 5000);
  };

  const handlePublishOverall = () => {
    setMarksList(prev => prev.map(m => ({ ...m, published: true })));
    setPublishBanner(`Successfully published ALL exam results overall! All students can now view their updated report cards.`);
    setTimeout(() => setPublishBanner(null), 5000);
  };

  const handleOpenEditMark = (mark: typeof marksList[0]) => {
    setEditingMarkId(mark.id);
    setEditMarksVal(mark.marksObtained);
    setIsEditModalOpen(true);
  };

  const handleSaveEditMark = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingMarkId === null) return;
    setMarksList(prev => prev.map(m => m.id === editingMarkId ? { ...m, marksObtained: editMarksVal } : m));
    setIsEditModalOpen(false);
    setEditingMarkId(null);
  };

  // Modals for adding items
  const [isCourseModalOpen, setIsCourseModalOpen] = useState(false);
  const [isVideoModalOpen, setIsVideoModalOpen] = useState(false);
  const [isDocModalOpen, setIsDocModalOpen] = useState(false);
  const [isBlogModalOpen, setIsBlogModalOpen] = useState(false);

  // Form states
  const [newCourseName, setNewCourseName] = useState('');
  const [newCourseCategory, setNewCourseCategory] = useState<'job' | 'entrance'>('job');
  const [newCourseFee, setNewCourseFee] = useState('');
  const [newCourseDur, setNewCourseDur] = useState('');
  
  const [newVideoName, setNewVideoName] = useState('');
  const [newVideoCourse, setNewVideoCourse] = useState('embedded-systems');
  const [newVideoDur, setNewVideoDur] = useState('');
  const [newVideoUrl, setNewVideoUrl] = useState('');

  const [newDocName, setNewDocName] = useState('');
  const [newDocCat, setNewDocCat] = useState<'Study Materials' | 'Course Notes' | 'Previous Papers' | 'Reference Guides'>('Study Materials');
  const [newDocSize, setNewDocSize] = useState('');
  const [newDocFile, setNewDocFile] = useState('');

  const [newBlogName, setNewBlogName] = useState('');
  const [newBlogCat, setNewBlogCat] = useState<'Embedded Systems' | 'Linux' | 'Data Analytics' | 'Career Guidance' | 'GATE Preparation'>('Embedded Systems');
  const [newBlogSummary, setNewBlogSummary] = useState('');
  const [newBlogContent, setNewBlogContent] = useState('');

  const menuItems = [
    { id: 'dashboard', name: 'Overview', icon: <LayoutDashboard className="h-5 w-5" /> },
    { id: 'students', name: 'Students Manager', icon: <Users className="h-5 w-5" /> },
    { id: 'marks', name: 'Marks & Results', icon: <CheckCircle className="h-5 w-5" /> },
    { id: 'courses', name: 'Course Catalog', icon: <BookOpen className="h-5 w-5" /> },
    { id: 'videos', name: 'Lecture Media', icon: <VideoIcon className="h-5 w-5" /> },
    { id: 'documents', name: 'Documents Center', icon: <FileText className="h-5 w-5" /> },
    { id: 'blogs', name: 'Blog Publications', icon: <Newspaper className="h-5 w-5" /> },
    { id: 'payments', name: 'Billing Records', icon: <CreditCard className="h-5 w-5" /> },
  ] as const;

  // Protect Route: Redirect if not logged in or role is not admin
  if (!user || user.role !== 'admin') {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <Card className="max-w-md w-full text-center p-6 space-y-4 shadow-md">
          <GraduationCap className="h-12 w-12 text-blue-600 mx-auto" />
          <h2 className="text-xl font-bold text-slate-900">Admin Access Required</h2>
          <p className="text-sm text-slate-500">This sector is restricted to coordinators. Please log in as an administrator to unlock metrics.</p>
          <Link href="/login">
            <Button className="w-full">Redirect to Login</Button>
          </Link>
        </Card>
      </div>
    );
  }

  // Mock Telemetry Records
  const mockStudents = [
    { id: 'st-01', name: 'Rahul Kumar', email: 'rahul.kumar@gmail.com', qualification: 'B.Tech CSE', city: 'Bangalore', course: 'Embedded Systems', date: '2026-06-21', status: 'Active' },
    { id: 'st-02', name: 'Priyanjali Roy', email: 'priyanjali.roy@outlook.com', qualification: 'M.Tech VLSI', city: 'Kolkata', course: 'Linux Kernel', date: '2026-06-20', status: 'Active' },
    { id: 'st-03', name: 'Anubhav Misra', email: 'anubhav.misra@yahoo.com', qualification: 'B.Tech ECE', city: 'Pune', course: 'Linux Device Drivers', date: '2026-06-18', status: 'Active' },
    { id: 'st-04', name: 'Siddharth Sen', email: 'siddharth@gmail.com', qualification: 'B.Sc Physics', city: 'Delhi', course: 'Data Analytics', date: '2026-06-15', status: 'Active' },
    { id: 'st-05', name: 'Trupti Patil', email: 'trupti.patil@gmail.com', qualification: 'B.Tech IT', city: 'Mumbai', course: 'GATE', date: '2026-06-14', status: 'Pending' }
  ];

  const mockPayments = [
    { id: 'txn-101', student: 'Rahul Kumar', course: 'Embedded Systems', fee: '₹34,999', method: 'UPI (GPay)', date: '2026-06-21', status: 'Success' },
    { id: 'txn-102', student: 'Priyanjali Roy', course: 'Linux Kernel', fee: '₹39,999', method: 'Credit Card', date: '2026-06-20', status: 'Success' },
    { id: 'txn-103', student: 'Anubhav Misra', course: 'Linux Device Drivers', fee: '₹37,999', method: 'Net Banking', date: '2026-06-18', status: 'Success' },
    { id: 'txn-104', student: 'Siddharth Sen', course: 'Data Analytics', fee: '₹29,999', method: 'UPI (PhonePe)', date: '2026-06-15', status: 'Success' },
    { id: 'txn-105', student: 'Amit Negi', course: 'ADS (Algorithms)', fee: '₹9,999', method: 'UPI', date: '2026-06-12', status: 'Success' }
  ];

  const handleAddCourse = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCourseName || !newCourseFee || !newCourseDur) {
      alert('Please fill out all fields.');
      return;
    }
    alert(`Success: Course "${newCourseName}" registered in system inventory!`);
    setIsCourseModalOpen(false);
    setNewCourseName('');
    setNewCourseFee('');
    setNewCourseDur('');
  };

  const handleAddVideo = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newVideoName || !newVideoDur || !newVideoUrl) {
      alert('Please fill out all fields.');
      return;
    }
    alert(`Success: Lecture clip "${newVideoName}" linked to course streams!`);
    setIsVideoModalOpen(false);
    setNewVideoName('');
    setNewVideoDur('');
    setNewVideoUrl('');
  };

  const handleAddDoc = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDocName || !newDocSize || !newDocFile) {
      alert('Please fill out all fields.');
      return;
    }
    alert(`Success: PDF Resource "${newDocName}" loaded to study materials library!`);
    setIsDocModalOpen(false);
    setNewDocName('');
    setNewDocSize('');
    setNewDocFile('');
  };

  const handleAddBlog = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBlogName || !newBlogSummary || !newBlogContent) {
      alert('Please fill out all fields.');
      return;
    }
    alert(`Success: Article "${newBlogName}" published to student feeds!`);
    setIsBlogModalOpen(false);
    setNewBlogName('');
    setNewBlogSummary('');
    setNewBlogContent('');
  };

  const menuItems = [
    { id: 'dashboard', name: 'Overview', icon: <LayoutDashboard className="h-5 w-5" /> },
    { id: 'students', name: 'Students Manager', icon: <Users className="h-5 w-5" /> },
    { id: 'courses', name: 'Course Catalog', icon: <BookOpen className="h-5 w-5" /> },
    { id: 'videos', name: 'Lecture Media', icon: <VideoIcon className="h-5 w-5" /> },
    { id: 'documents', name: 'Documents Center', icon: <FileText className="h-5 w-5" /> },
    { id: 'blogs', name: 'Blog Publications', icon: <Newspaper className="h-5 w-5" /> },
    { id: 'payments', name: 'Billing Records', icon: <CreditCard className="h-5 w-5" /> },
  ] as const;

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col md:flex-row" id="admin-wrapper">
      
      {/* MOBILE HEADER BAR */}
      <div className="md:hidden bg-slate-900 text-white flex justify-between items-center px-4 py-3 sticky top-0 z-40">
        <div className="flex items-center gap-2">
          <GraduationCap className="h-6 w-6 text-blue-400" />
          <span className="font-bold">EnviBytes Coordinator</span>
        </div>
        <button onClick={() => setSidebarOpen(!sidebarOpen)} className="p-1 rounded-lg border border-slate-700">
          {sidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* LEFT SIDEBAR PANEL */}
      <aside className={`fixed inset-y-0 left-0 z-30 w-64 bg-slate-900 text-slate-300 flex flex-col transform transition-transform duration-200 md:relative md:transform-none ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      }`}>
        {/* Brand Banner */}
        <div className="h-16 flex items-center gap-2.5 px-6 border-b border-slate-800 shrink-0">
          <div className="h-8 w-8 rounded-lg bg-blue-600 text-white flex items-center justify-center">
            <GraduationCap className="h-5 w-5" />
          </div>
          <span className="text-white font-bold tracking-wider text-base">EnviBytes</span>
          <Badge variant="warning" className="text-[9px] bg-slate-800 border-none text-amber-400 uppercase py-0.5">ADMIN</Badge>
        </div>

        {/* User Card */}
        <div className="p-4 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-slate-800 text-white flex items-center justify-center font-bold border border-slate-700">
              A
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-semibold text-white truncate">{user.name}</span>
              <span className="text-[10px] text-slate-400 truncate">System Manager</span>
            </div>
          </div>
        </div>

        {/* Sidebar Links */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {menuItems.map((item) => (
            <button
              key={item.id}
              onClick={() => {
                setActiveTab(item.id);
                setSidebarOpen(false);
              }}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === item.id
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/10 font-semibold'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
              id={`admin-sidebar-${item.id}`}
            >
              {item.icon}
              <span>{item.name}</span>
            </button>
          ))}
        </nav>

        {/* Logout at bottom */}
        <div className="p-4 border-t border-slate-800 shrink-0">
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:bg-red-950/40 hover:text-red-400 transition-colors"
            id="admin-sidebar-logout"
          >
            <LogOut className="h-5 w-5" />
            <span>Logout Panel</span>
          </button>
        </div>
      </aside>

      {/* RIGHT WORKSPACE AREA */}
      <main className="flex-1 flex flex-col min-w-0">
        
        {/* TOP NAV BAR */}
        <header className="hidden md:flex h-16 items-center justify-between px-8 bg-white border-b border-slate-200 sticky top-0 z-20 shadow-sm">
          <h2 className="font-bold text-slate-800 capitalize text-lg">
            Coordinator Panel &mdash; {activeTab.replace(/([A-Z])/g, ' $1')}
          </h2>
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-400 font-medium">Gateway: Live API v1.0</span>
            <div className="h-4 w-px bg-slate-200" />
            <Badge variant="success" className="text-[10px] tracking-wide">SYSTEM OK</Badge>
          </div>
        </header>

        {/* WORKSPACE CONTENTS */}
        <div className="flex-1 p-6 md:p-8 overflow-y-auto">

          {/* TAB 1: OVERVIEW DASHBOARD */}
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              
              {/* Analytics Stat Blocks */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                
                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="space-y-1">
                      <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Total Students</p>
                      <h4 className="text-3xl font-bold text-slate-900">1,245</h4>
                    </div>
                    <div className="h-12 w-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                      <Users className="h-6 w-6" />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="space-y-1">
                      <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Total Revenue</p>
                      <h4 className="text-3xl font-bold text-slate-900">₹4,85,000</h4>
                    </div>
                    <div className="h-12 w-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                      <CreditCard className="h-6 w-6" />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="space-y-1">
                      <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Active Courses</p>
                      <h4 className="text-3xl font-bold text-slate-900">{coursesData.length}</h4>
                    </div>
                    <div className="h-12 w-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                      <BookOpen className="h-6 w-6" />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-6 flex items-center justify-between">
                    <div className="space-y-1">
                      <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Monthly Sales</p>
                      <h4 className="text-3xl font-bold text-slate-900">148</h4>
                    </div>
                    <div className="h-12 w-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                      <TrendingUp className="h-6 w-6" />
                    </div>
                  </CardContent>
                </Card>

              </div>

              {/* Analytics Chart & Telemetry split */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                
                {/* SVG Revenue Telemetry Chart */}
                <div className="lg:col-span-8 space-y-4">
                  <h3 className="text-base font-bold text-slate-800">Sales Trend (Last 6 Months)</h3>
                  <Card className="p-6 bg-white">
                    <div className="aspect-[2/1] w-full relative flex flex-col justify-between">
                      {/* Custom SVG line graph chart */}
                      <svg className="w-full h-full min-h-[220px]" viewBox="0 0 500 220" fill="none" xmlns="http://www.w3.org/2000/svg">
                        {/* Horizontal grid lines */}
                        <line x1="40" y1="40" x2="480" y2="40" stroke="#F1F5F9" strokeWidth="1" />
                        <line x1="40" y1="90" x2="480" y2="90" stroke="#F1F5F9" strokeWidth="1" />
                        <line x1="40" y1="140" x2="480" y2="140" stroke="#F1F5F9" strokeWidth="1" />
                        <line x1="40" y1="180" x2="480" y2="180" stroke="#E2E8F0" strokeWidth="1.5" />
                        
                        {/* Labels Y */}
                        <text x="10" y="45" fill="#94A3B8" fontSize="10" fontFamily="sans-serif">₹5.0L</text>
                        <text x="10" y="95" fill="#94A3B8" fontSize="10" fontFamily="sans-serif">₹3.0L</text>
                        <text x="10" y="145" fill="#94A3B8" fontSize="10" fontFamily="sans-serif">₹1.0L</text>

                        {/* Chart Line path */}
                        <path d="M40 180L120 150L200 130L280 80L360 95L440 50" stroke="#2563EB" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
                        {/* Gradient Fill under Path */}
                        <path d="M40 180L120 150L200 130L280 80L360 95L440 50V180H40Z" fill="url(#blueGrad)" opacity="0.08" />
                        <defs>
                          <linearGradient id="blueGrad" x1="240" y1="50" x2="240" y2="180" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#2563EB" />
                            <stop offset="1" stopColor="#FFFFFF" />
                          </linearGradient>
                        </defs>

                        {/* Chart Dots */}
                        <circle cx="120" cy="150" r="4.5" fill="#2563EB" stroke="#FFFFFF" strokeWidth="1.5" />
                        <circle cx="200" cy="130" r="4.5" fill="#2563EB" stroke="#FFFFFF" strokeWidth="1.5" />
                        <circle cx="280" cy="80" r="4.5" fill="#2563EB" stroke="#FFFFFF" strokeWidth="1.5" />
                        <circle cx="360" cy="95" r="4.5" fill="#2563EB" stroke="#FFFFFF" strokeWidth="1.5" />
                        <circle cx="440" cy="50" r="4.5" fill="#2563EB" stroke="#FFFFFF" strokeWidth="1.5" />

                        {/* Labels X */}
                        <text x="110" y="200" fill="#64748B" fontSize="10" fontFamily="sans-serif">Jan</text>
                        <text x="190" y="200" fill="#64748B" fontSize="10" fontFamily="sans-serif">Feb</text>
                        <text x="270" y="200" fill="#64748B" fontSize="10" fontFamily="sans-serif">Mar</text>
                        <text x="350" y="200" fill="#64748B" fontSize="10" fontFamily="sans-serif">Apr</text>
                        <text x="430" y="200" fill="#64748B" fontSize="10" fontFamily="sans-serif">May</text>
                      </svg>
                    </div>
                  </Card>
                </div>

                {/* Right: Summary logs */}
                <div className="lg:col-span-4 space-y-4">
                  <h3 className="text-base font-bold text-slate-800">Quick Metrics</h3>
                  <Card className="p-5 bg-white space-y-4">
                    <div className="space-y-3">
                      <div className="flex justify-between items-center text-xs">
                        <span className="text-slate-500 font-medium">LMS Server Status:</span>
                        <Badge variant="success">ONLINE</Badge>
                      </div>
                      <div className="flex justify-between items-center text-xs">
                        <span className="text-slate-500 font-medium">Database Load:</span>
                        <span className="font-semibold text-slate-700">12% Cpu</span>
                      </div>
                      <div className="flex justify-between items-center text-xs">
                        <span className="text-slate-500 font-medium">SSL Certificate:</span>
                        <span className="text-slate-700 font-medium flex items-center gap-1">
                          🟢 Secure
                        </span>
                      </div>
                    </div>
                    <div className="border-t border-slate-100 pt-4 text-center">
                      <button onClick={() => alert('Simulated full system backup started...')} className="text-xs text-blue-600 hover:text-blue-700 font-bold">
                        Trigger System Backup
                      </button>
                    </div>
                  </Card>
                </div>

              </div>

              {/* Recent Registrations Table */}
              <div className="space-y-4">
                <h3 className="text-base font-bold text-slate-800">Recent Registrations</h3>
                <Card className="overflow-hidden bg-white shadow-sm">
                  <div className="overflow-x-auto">
                    <table className="w-full border-collapse text-left text-xs">
                      <thead className="bg-slate-50 text-slate-500 border-b border-slate-150">
                        <tr>
                          <th className="p-4 font-bold">Student</th>
                          <th className="p-4 font-bold">Qualification</th>
                          <th className="p-4 font-bold">City</th>
                          <th className="p-4 font-bold">Course Focus</th>
                          <th className="p-4 font-bold">Register Date</th>
                          <th className="p-4 font-bold text-right">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 text-slate-700">
                        {mockStudents.map((st) => (
                          <tr key={st.id} className="hover:bg-slate-50/50 transition-colors">
                            <td className="p-4 font-semibold text-slate-900">{st.name}</td>
                            <td className="p-4">{st.qualification}</td>
                            <td className="p-4">{st.city}</td>
                            <td className="p-4">{st.course}</td>
                            <td className="p-4 font-mono text-slate-500">{st.date}</td>
                            <td className="p-4 text-right">
                              <Badge variant={st.status === 'Active' ? 'success' : 'warning'}>
                                {st.status}
                              </Badge>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Card>
              </div>

            </div>
          )}

          {/* TAB 2: STUDENTS MANAGER */}
          {activeTab === 'students' && (
            <div className="space-y-6 animate-fade-in">
              <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
                <h3 className="text-base font-bold text-slate-800">Student Directory ({mockStudents.length} entries)</h3>
                {/* Search */}
                <div className="relative max-w-xs w-full">
                  <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search student directories..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-4 py-1.5 border border-slate-200 rounded-lg text-xs bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  />
                </div>
              </div>

              <Card className="overflow-hidden bg-white">
                <table className="w-full border-collapse text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 border-b border-slate-150">
                    <tr>
                      <th className="p-4 font-bold">Name</th>
                      <th className="p-4 font-bold">Email</th>
                      <th className="p-4 font-bold">Qualification</th>
                      <th className="p-4 font-bold">Location</th>
                      <th className="p-4 font-bold">Interests</th>
                      <th className="p-4 font-bold text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {mockStudents
                      .filter((st) => st.name.toLowerCase().includes(searchQuery.toLowerCase()))
                      .map((st) => (
                        <tr key={st.id} className="hover:bg-slate-50/50">
                          <td className="p-4 font-semibold text-slate-900">{st.name}</td>
                          <td className="p-4 font-mono text-slate-500">{st.email}</td>
                          <td className="p-4">{st.qualification}</td>
                          <td className="p-4">{st.city}</td>
                          <td className="p-4">{st.course}</td>
                          <td className="p-4 text-right">
                            <button onClick={() => alert(`Details for ${st.name}: ${st.qualification}, ${st.city}`)} className="text-blue-600 hover:text-blue-700 font-bold mr-3">
                              Manage
                            </button>
                            <button onClick={() => alert(`Suspended ${st.name}`)} className="text-red-500 hover:text-red-600">
                              Suspend
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </Card>
            </div>
          )}

          {/* TAB 3: COURSE CATALOG */}
          {activeTab === 'courses' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-bold text-slate-800">System Course Catalogs</h3>
                <Button size="sm" onClick={() => setIsCourseModalOpen(true)} className="gap-1 font-semibold" id="btn-add-course-modal">
                  <Plus className="h-4 w-4" /> Add Course
                </Button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {coursesData.map((course) => (
                  <Card key={course.id} className="p-5 flex flex-col justify-between h-full">
                    <div className="space-y-3">
                      <div className="flex justify-between items-center">
                        <Badge variant={course.category === 'job' ? 'primary' : 'warning'}>
                          {course.category}
                        </Badge>
                        <span className="font-mono text-xs text-slate-400">ID: {course.id}</span>
                      </div>
                      <h4 className="font-bold text-slate-900 leading-snug">{course.title}</h4>
                      <p className="text-slate-500 text-xs line-clamp-3 leading-relaxed">{course.description}</p>
                    </div>

                    <div className="border-t border-slate-100 mt-4 pt-3 flex items-center justify-between text-xs">
                      <span className="font-bold text-slate-950">{course.fee}</span>
                      <button onClick={() => alert(`Edit config for: ${course.title}`)} className="text-blue-600 hover:text-blue-700 font-semibold">
                        Edit Settings
                      </button>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* TAB 4: LECTURE MEDIA */}
          {activeTab === 'videos' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-bold text-slate-800">Lecture Clips Index</h3>
                <Button size="sm" onClick={() => setIsVideoModalOpen(true)} className="gap-1 font-semibold" id="btn-add-video-modal">
                  <Plus className="h-4 w-4" /> Add Lecture
                </Button>
              </div>

              <Card className="overflow-hidden bg-white">
                <table className="w-full border-collapse text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 border-b border-slate-150">
                    <tr>
                      <th className="p-4 font-bold">Clip Title</th>
                      <th className="p-4 font-bold">Course Path</th>
                      <th className="p-4 font-bold">Duration</th>
                      <th className="p-4 font-bold">Role Access</th>
                      <th className="p-4 font-bold text-right">Metrics</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {videosData.map((video) => (
                      <tr key={video.id} className="hover:bg-slate-50/50">
                        <td className="p-4 font-semibold text-slate-900">{video.title}</td>
                        <td className="p-4">{video.courseTitle}</td>
                        <td className="p-4 font-mono text-slate-500">{video.duration}</td>
                        <td className="p-4">
                          <Badge variant={video.isSample ? 'success' : 'primary'}>
                            {video.isSample ? 'Free Preview' : 'Members'}
                          </Badge>
                        </td>
                        <td className="p-4 text-right text-slate-500">{video.views} plays</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            </div>
          )}

          {/* TAB 5: DOCUMENTS CENTER */}
          {activeTab === 'documents' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-bold text-slate-800">Study Files & PDF Library</h3>
                <Button size="sm" onClick={() => setIsDocModalOpen(true)} className="gap-1 font-semibold" id="btn-add-doc-modal">
                  <Plus className="h-4 w-4" /> Add PDF Booklet
                </Button>
              </div>

              <Card className="overflow-hidden bg-white">
                <table className="w-full border-collapse text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 border-b border-slate-150">
                    <tr>
                      <th className="p-4 font-bold">Document Title</th>
                      <th className="p-4 font-bold">Category Shelf</th>
                      <th className="p-4 font-bold">File Size</th>
                      <th className="p-4 font-bold">System File Link</th>
                      <th className="p-4 font-bold text-right">Downloads</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {documentsData.map((doc) => (
                      <tr key={doc.id} className="hover:bg-slate-50/50">
                        <td className="p-4 font-semibold text-slate-900">{doc.title}</td>
                        <td className="p-4">{doc.category}</td>
                        <td className="p-4 text-slate-500">{doc.fileSize}</td>
                        <td className="p-4 font-mono text-[10px] text-slate-400">{doc.fileName}</td>
                        <td className="p-4 text-right font-semibold text-blue-600">{doc.downloadCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            </div>
          )}

          {/* TAB 6: BLOG PUBLICATIONS */}
          {activeTab === 'blogs' && (
            <div className="space-y-6">
              <div className="flex justify-between items-center">
                <h3 className="text-base font-bold text-slate-800">Blog Articles</h3>
                <Button size="sm" onClick={() => setIsBlogModalOpen(true)} className="gap-1 font-semibold" id="btn-add-blog-modal">
                  <Plus className="h-4 w-4" /> Publish Blog
                </Button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {blogsData.map((blog) => (
                  <Card key={blog.id} className="p-5 flex flex-col justify-between h-full">
                    <div className="space-y-2">
                      <div className="flex justify-between items-center text-xs">
                        <span className="font-semibold text-blue-600 uppercase tracking-wide text-[10px]">{blog.category}</span>
                        <span className="text-slate-400 font-mono">{blog.date}</span>
                      </div>
                      <h4 className="font-bold text-slate-900 leading-snug line-clamp-1">{blog.title}</h4>
                      <p className="text-slate-500 text-xs line-clamp-2 leading-relaxed">{blog.summary}</p>
                    </div>

                    <div className="border-t border-slate-100 mt-4 pt-3 flex items-center justify-between text-xs">
                      <span className="text-slate-400">By: {blog.author}</span>
                      <button onClick={() => alert(`Editing: ${blog.title}`)} className="text-blue-600 hover:text-blue-700 font-semibold">
                        Edit Article
                      </button>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* TAB 7: BILLING RECORDS */}
          {activeTab === 'payments' && (
            <div className="space-y-6">
              <h3 className="text-base font-bold text-slate-800">Sales Transactions Ledger</h3>
              
              <Card className="overflow-hidden bg-white">
                <table className="w-full border-collapse text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 border-b border-slate-150">
                    <tr>
                      <th className="p-4 font-bold">Transaction ID</th>
                      <th className="p-4 font-bold">Student</th>
                      <th className="p-4 font-bold">Associated Course</th>
                      <th className="p-4 font-bold">Amount Paid</th>
                      <th className="p-4 font-bold">Method</th>
                      <th className="p-4 font-bold font-mono">Date</th>
                      <th className="p-4 font-bold text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {mockPayments.map((pay) => (
                      <tr key={pay.id} className="hover:bg-slate-50/50">
                        <td className="p-4 font-mono font-semibold text-slate-900">{pay.id}</td>
                        <td className="p-4 font-medium">{pay.student}</td>
                        <td className="p-4">{pay.course}</td>
                        <td className="p-4 font-bold text-slate-900">{pay.fee}</td>
                        <td className="p-4 text-slate-500">{pay.method}</td>
                        <td className="p-4 font-mono text-slate-400">{pay.date}</td>
                        <td className="p-4 text-right">
                          <Badge variant="success">{pay.status}</Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            </div>
          )}

          {/* TAB 8: MARKS & RESULTS MANAGEMENT */}
          {activeTab === 'marks' && (
            <div className="space-y-6">
              {publishBanner && (
                <div className="bg-emerald-600 text-white px-4 py-3 rounded-xl shadow-lg flex items-center justify-between animate-fadeIn">
                  <div className="flex items-center gap-2">
                    <CheckCircle className="h-5 w-5 text-emerald-200" />
                    <span className="text-xs font-semibold">{publishBanner}</span>
                  </div>
                  <button onClick={() => setPublishBanner(null)} className="text-white/80 hover:text-white">
                    <X className="h-4 w-4" />
                  </button>
                </div>
              )}

              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
                <div>
                  <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                    <CheckCircle className="h-5 w-5 text-blue-600" />
                    Marks & Results Approval Center
                  </h2>
                  <p className="text-xs text-slate-500 mt-1">
                    Review student marks entered by teachers/admins. Edit scores before publishing class-wise or overall.
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <button
                    onClick={() => handlePublishClassWise(selectedClassFilter)}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-md shadow-blue-500/20 transition-all flex items-center gap-2"
                  >
                    <CheckCircle className="h-4 w-4" />
                    Publish Class Results {selectedClassFilter !== 'All' ? `(${selectedClassFilter})` : ''}
                  </button>
                  <button
                    onClick={handlePublishOverall}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-md shadow-emerald-500/20 transition-all flex items-center gap-2"
                  >
                    <TrendingUp className="h-4 w-4" />
                    Publish Overall Results
                  </button>
                </div>
              </div>

              {/* Filters Bar */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-white p-3 rounded-xl border border-slate-200 flex items-center gap-3">
                  <span className="text-xs font-semibold text-slate-600">Class Filter:</span>
                  <select
                    value={selectedClassFilter}
                    onChange={(e) => setSelectedClassFilter(e.target.value)}
                    className="flex-1 bg-slate-50 border border-slate-200 text-xs rounded-lg px-2.5 py-1.5 focus:outline-none text-slate-800 font-medium"
                  >
                    <option value="All">All Classes</option>
                    <option value="Class 10-A">Class 10-A</option>
                    <option value="Class 10-B">Class 10-B</option>
                    <option value="Class 9-A">Class 9-A</option>
                  </select>
                </div>

                <div className="bg-white p-3 rounded-xl border border-slate-200 flex items-center gap-3">
                  <span className="text-xs font-semibold text-slate-600">Exam Filter:</span>
                  <select
                    value={selectedExamFilter}
                    onChange={(e) => setSelectedExamFilter(e.target.value)}
                    className="flex-1 bg-slate-50 border border-slate-200 text-xs rounded-lg px-2.5 py-1.5 focus:outline-none text-slate-800 font-medium"
                  >
                    <option value="All">All Examinations</option>
                    <option value="Mid-Term Examination 2026">Mid-Term Examination 2026</option>
                    <option value="Unit Test 1">Unit Test 1</option>
                  </select>
                </div>

                <div className="bg-white p-3 rounded-xl border border-slate-200 flex items-center gap-2">
                  <Search className="h-4 w-4 text-slate-400 shrink-0" />
                  <input
                    type="text"
                    placeholder="Search by student name or ID..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full text-xs bg-transparent focus:outline-none text-slate-800"
                  />
                </div>
              </div>

              {/* Marks Table */}
              <Card>
                <table className="w-full border-collapse text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider text-[10px]">
                    <tr>
                      <th className="p-4 font-bold">Student ID</th>
                      <th className="p-4 font-bold">Student Name</th>
                      <th className="p-4 font-bold">Class</th>
                      <th className="p-4 font-bold">Exam</th>
                      <th className="p-4 font-bold">Subject</th>
                      <th className="p-4 font-bold text-center">Marks Obtained</th>
                      <th className="p-4 font-bold text-center">Status</th>
                      <th className="p-4 font-bold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {marksList
                      .filter(m => selectedClassFilter === 'All' || m.className === selectedClassFilter)
                      .filter(m => selectedExamFilter === 'All' || m.examName === selectedExamFilter)
                      .filter(m => !searchQuery || m.studentName.toLowerCase().includes(searchQuery.toLowerCase()) || m.studentId.toLowerCase().includes(searchQuery.toLowerCase()))
                      .map((mark) => (
                        <tr key={mark.id} className="hover:bg-slate-50/50">
                          <td className="p-4 font-mono font-semibold text-slate-900">{mark.studentId}</td>
                          <td className="p-4 font-semibold text-slate-800">{mark.studentName}</td>
                          <td className="p-4 text-slate-600">{mark.className}</td>
                          <td className="p-4 font-medium text-slate-700">{mark.examName}</td>
                          <td className="p-4 text-slate-600">{mark.subject}</td>
                          <td className="p-4 text-center font-bold text-slate-900">
                            {mark.marksObtained} / {mark.maxMarks}
                          </td>
                          <td className="p-4 text-center">
                            {mark.published ? (
                              <Badge variant="success" className="inline-flex items-center gap-1">
                                <CheckCircle className="h-3 w-3" /> Published
                              </Badge>
                            ) : (
                              <Badge variant="warning" className="inline-flex items-center gap-1 bg-amber-50 text-amber-700 border-amber-200">
                                <AlertCircle className="h-3 w-3" /> Draft (Pending)
                              </Badge>
                            )}
                          </td>
                          <td className="p-4 text-right">
                            <button
                              onClick={() => handleOpenEditMark(mark)}
                              className="text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold px-3 py-1.5 rounded-lg border border-slate-200 transition-colors"
                            >
                              Edit Score
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </Card>
            </div>
          )}

        </div>
      </main>

      {/* --- ADD COURSE MODAL --- */}
      <Modal isOpen={isCourseModalOpen} onClose={() => setIsCourseModalOpen(false)} title="Register New Course in System">
        <form onSubmit={handleAddCourse} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Course Name</label>
            <input
              type="text"
              placeholder="e.g. Linux System Programming"
              value={newCourseName}
              onChange={(e) => setNewCourseName(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Category</label>
              <select
                value={newCourseCategory}
                onChange={(e) => setNewCourseCategory(e.target.value as 'job' | 'entrance')}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                <option value="job">💼 Job Course</option>
                <option value="entrance">🎓 Entrance prep</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Fees (INR)</label>
              <input
                type="text"
                placeholder="₹24,999"
                value={newCourseFee}
                onChange={(e) => setNewCourseFee(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Duration</label>
            <input
              type="text"
              placeholder="e.g. 5 Months"
              value={newCourseDur}
              onChange={(e) => setNewCourseDur(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsCourseModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" id="btn-submit-add-course">
              Register Course
            </Button>
          </div>
        </form>
      </Modal>

      {/* --- ADD LECTURE MODAL --- */}
      <Modal isOpen={isVideoModalOpen} onClose={() => setIsVideoModalOpen(false)} title="Link New Lecture Video File">
        <form onSubmit={handleAddVideo} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Video Lecture Title</label>
            <input
              type="text"
              placeholder="e.g. Debugging with Logic Analyzers"
              value={newVideoName}
              onChange={(e) => setNewVideoName(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Link to Course</label>
              <select
                value={newVideoCourse}
                onChange={(e) => setNewVideoCourse(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                {coursesData.map((c) => (
                  <option key={c.id} value={c.id}>{c.title}</option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Duration</label>
              <input
                type="text"
                placeholder="e.g. 24:15"
                value={newVideoDur}
                onChange={(e) => setNewVideoDur(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Video Embed URL</label>
            <input
              type="text"
              placeholder="https://youtube.com/embed/..."
              value={newVideoUrl}
              onChange={(e) => setNewVideoUrl(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsVideoModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" id="btn-submit-add-video">
              Link Video
            </Button>
          </div>
        </form>
      </Modal>

      {/* --- ADD DOCUMENT MODAL --- */}
      <Modal isOpen={isDocModalOpen} onClose={() => setIsDocModalOpen(false)} title="Upload PDF Note Resource">
        <form onSubmit={handleAddDoc} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Document Title</label>
            <input
              type="text"
              placeholder="e.g. FreeRTOS Scheduling Internals"
              value={newDocName}
              onChange={(e) => setNewDocName(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Category Shelf</label>
              <select
                value={newDocCat}
                onChange={(e) => setNewDocCat(e.target.value as any)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                <option value="Study Materials">Study Materials</option>
                <option value="Course Notes">Course Notes</option>
                <option value="Previous Papers">Previous Papers</option>
                <option value="Reference Guides">Reference Guides</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">File Size (MB)</label>
              <input
                type="text"
                placeholder="e.g. 2.4 MB"
                value={newDocSize}
                onChange={(e) => setNewDocSize(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Filename</label>
            <input
              type="text"
              placeholder="e.g. FreeRTOS_Scheduling_Internals.pdf"
              value={newDocFile}
              onChange={(e) => setNewDocFile(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsDocModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" id="btn-submit-add-doc">
              Load PDF
            </Button>
          </div>
        </form>
      </Modal>

      {/* --- ADD BLOG MODAL --- */}
      <Modal isOpen={isBlogModalOpen} onClose={() => setIsBlogModalOpen(false)} title="Publish Technical Blog Article">
        <form onSubmit={handleAddBlog} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Blog Title</label>
            <input
              type="text"
              placeholder="e.g. Understanding Slab vs Slub Allocators"
              value={newBlogName}
              onChange={(e) => setNewBlogName(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Category Tag</label>
            <select
              value={newBlogCat}
              onChange={(e) => setNewBlogCat(e.target.value as any)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              <option value="Embedded Systems">Embedded Systems</option>
              <option value="Linux">Linux</option>
              <option value="Data Analytics">Data Analytics</option>
              <option value="Career Guidance">Career Guidance</option>
              <option value="GATE Preparation">GATE Preparation</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Short Summary</label>
            <input
              type="text"
              placeholder="Brief 1-sentence descriptor..."
              value={newBlogSummary}
              onChange={(e) => setNewBlogSummary(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Article Content (Markdown)</label>
            <textarea
              placeholder="Write the full article body..."
              rows={4}
              value={newBlogContent}
              onChange={(e) => setNewBlogContent(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsBlogModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" id="btn-submit-add-blog">
              Publish Article
            </Button>
          </div>
        </form>
      {/* --- EDIT MARK MODAL --- */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Student Mark Entry">
        <form onSubmit={handleSaveEditMark} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">Marks Obtained</label>
            <input
              type="number"
              value={editMarksVal}
              onChange={(e) => setEditMarksVal(Number(e.target.value))}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm">
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

    </div>
  );
}
