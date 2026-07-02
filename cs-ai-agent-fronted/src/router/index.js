import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import VisionQaApp from '../views/VisionQaApp.vue'
import VisionAgentApp from '../views/VisionAgentApp.vue'
import DialogWorkspace from '../views/DialogWorkspace.vue'
import CollectionWorkspace from '../views/CollectionWorkspace.vue'
import CollectionPlanner from '../views/CollectionPlanner.vue'
import ReportCenter from '../views/ReportCenter.vue'
import ProjectWorkspace from '../views/ProjectWorkspace.vue'
import DebugPage from '../views/DebugPage.vue'
import LoginPage from '../views/LoginPage.vue'
import KnowledgePage from '../views/KnowledgePage.vue'

const routes = [
  { path: '/login', name: 'LoginPage', component: LoginPage, meta: { public: true } },
  { path: '/', name: 'Home', component: Home },
  { path: '/dialog-workspace', name: 'DialogWorkspace', component: DialogWorkspace },
  { path: '/collection-workspace', name: 'CollectionWorkspace', component: CollectionWorkspace },
  { path: '/vision-qa', name: 'VisionQaApp', component: VisionQaApp },
  { path: '/vision-agent', name: 'VisionAgentApp', component: VisionAgentApp },
  { path: '/collection-planner', name: 'CollectionPlanner', component: CollectionPlanner },
  { path: '/report-center', name: 'ReportCenter', component: ReportCenter },
  { path: '/project', name: 'ProjectWorkspace', component: ProjectWorkspace },
  { path: '/knowledge', name: 'KnowledgePage', component: KnowledgePage },
  { path: '/debug', name: 'DebugPage', component: DebugPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!localStorage.getItem('token')) return '/login'
  return true
})

export default router
