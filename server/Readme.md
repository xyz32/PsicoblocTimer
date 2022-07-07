### Install
Copy server.py to the "rasbperry pi"
Add script to the chron startup script:
`sudo crontab -e`
`@reboot python /home/pi/timer/server.py &`
