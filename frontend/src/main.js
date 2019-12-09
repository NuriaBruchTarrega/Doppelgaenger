import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false;

Vue.prototype.$backendHost = location.hostname + ':8888';

new Vue({
    render: h => h(App),
}).$mount('#app');
