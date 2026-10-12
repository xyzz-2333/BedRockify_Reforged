"""Compare chosen physics using Java's update order; this is not a Bedrock engine emulator."""
import math

def range_at_angle(speed, gravity, drag, angle):
    vx=speed*math.cos(math.radians(angle));vy=speed*math.sin(math.radians(angle))
    x=0.0;y=1.62
    for tick in range(1,2001):
        old_x,old_y=x,y
        x+=vx;y+=vy
        if y<=0:
            f=old_y/(old_y-y)
            return old_x+(x-old_x)*f,tick-1+f
        vx*=drag;vy=vy*drag-gravity
    raise ValueError('Trajectory did not land')

if __name__=='__main__':
    profiles={'Java pearl':(1.5,0.03,0.99),'Bedrock pearl parameters':(1.5,0.025,1.0),
              'Java trident':(2.5,0.05,0.99),'Bedrock trident parameters':(4.0,0.1,0.99)}
    print('Flat ground, launch height 1.62, stationary shooter, no spread or collisions.')
    print('Bedrock rows use chosen parameters in Java update order, not measured Bedrock trajectories.')
    for angle in (0,30,45,60):
        for name,args in profiles.items():
            distance,ticks=range_at_angle(*args,angle)
            print(f'{angle:2d} degrees | {name:27s} | {distance:8.3f} blocks | {ticks:7.3f} ticks')
