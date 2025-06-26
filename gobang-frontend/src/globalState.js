import { reactive } from 'vue'

export const globalState = reactive({
  user: {
    userId: '',
    nickname: '',
    email: ''
  },
  currentRoom: {
    roomId: '',
    status: 0
  }
}) 