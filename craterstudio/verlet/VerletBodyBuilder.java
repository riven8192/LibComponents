/*
 * Created on Aug 21, 2008
 */

package craterstudio.verlet;

import java.util.List;

public class VerletBodyBuilder
{
   public static VerletBody createBox(float sphereRadius, float overlap, int xUnits, int yUnits, int zUnits, float xOff, float yOff, float zOff)
   {
      VerletBody body = new VerletBody();

      float xDim = sphereRadius * xUnits - overlap * (xUnits - 1);
      float yDim = sphereRadius * yUnits - overlap * (yUnits - 1);
      float zDim = sphereRadius * zUnits - overlap * (zUnits - 1);

      float stiffness = 1.0f;
      int how = VerletSpring.ENFORCE_FIXED_LENGTH;

      VerletSphere[][][] grid = new VerletSphere[xUnits][yUnits][zUnits];

      for (int z = 0; z < zUnits; z++)
      {
         for (int y = 0; y < yUnits; y++)
         {
            for (int x = 0; x < xUnits; x++)
            {
               float xPos = xOff + (sphereRadius * x) - xDim / 2;
               float yPos = yOff + (sphereRadius * y) - yDim / 2;
               float zPos = zOff + (sphereRadius * z) - zDim / 2;

               VerletParticle corner = new VerletParticle();
               corner.setPosition(xPos, yPos, zPos);
               body.addSphere(grid[x][y][z] = new VerletSphere(corner, sphereRadius));
            }
         }
      }

      for (int z = 0; z < zUnits; z++)
      {
         for (int y = 0; y < yUnits; y++)
         {
            for (int x = 0; x < xUnits; x++)
            {
               // straight x,y,z
               if (x < xUnits - 1 && y < yUnits - 1 && z < zUnits - 1)
               {
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 1][y + 0][z + 0], stiffness, how);
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 0][y + 1][z + 0], stiffness, how);
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 0][y + 0][z + 1], stiffness, how);
               }

               // xy cross
               if (x < xUnits - 1 && y < yUnits - 1)
               {
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 1][y + 1][z + 0], stiffness, how);
                  body.createLocalSpring(grid[x + 1][y + 0][z + 0], grid[x + 0][y + 1][z + 0], stiffness, how);
               }

               // zy cross
               if (y < yUnits - 1 && z < zUnits - 1)
               {
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 0][y + 1][z + 1], stiffness, how);
                  body.createLocalSpring(grid[x + 0][y + 0][z + 1], grid[x + 0][y + 1][z + 0], stiffness, how);
               }

               // xz cross
               if (z < zUnits - 1 && x < xUnits - 1)
               {
                  body.createLocalSpring(grid[x + 0][y + 0][z + 0], grid[x + 1][y + 0][z + 1], stiffness, how);
                  body.createLocalSpring(grid[x + 1][y + 0][z + 0], grid[x + 0][y + 0][z + 1], stiffness, how);
               }
            }
         }
      }

      return body;
   }

   public static VerletBody createCube(float dim, float xOff, float yOff, float zOff, boolean hifi)
   {
      VerletBody body = new VerletBody();

      float centerRadius = dim * 0.5f;
      float cornerRadius = centerRadius * 0.33f;

      cornerRadius *= centerRadius;

      float stiffness = 1.0f;
      int how = VerletSpring.ENFORCE_FIXED_LENGTH;

      int inc = hifi ? 1 : 2;

      VerletSphere[][][] grid = new VerletSphere[3][3][3];

      for (int z = -1; z < 2; z += inc)
      {
         for (int y = -1; y < 2; y += inc)
         {
            for (int x = -1; x < 2; x += inc)
            {
               float xPos = xOff + x * centerRadius - x * cornerRadius;
               float yPos = yOff + y * centerRadius - y * cornerRadius;
               float zPos = zOff + z * centerRadius - z * cornerRadius;

               VerletParticle corner = new VerletParticle();
               corner.setPosition(xPos, yPos, zPos);
               body.addSphere(grid[x + 1][y + 1][z + 1] = new VerletSphere(corner, cornerRadius));
            }
         }
      }

      if (hifi)
      {
         for (int z = -1; z < 2; z++)
         {
            for (int y = -1; y < 2; y++)
            {
               for (int x = -1; x < 2; x++)
               {
                  // straight x,y,z
                  if (x != 1 && y != 1 && z != 1)
                  {
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 2][y + 1][z + 1], stiffness, how);
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 1][y + 2][z + 1], stiffness, how);
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 1][y + 1][z + 2], stiffness, how);
                  }

                  // xz cross
                  if (x != 1 && z != 1)
                  {
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 2][y + 1][z + 2], stiffness, how);
                     body.createLocalSpring(grid[x + 2][y + 1][z + 1], grid[x + 1][y + 1][z + 2], stiffness, how);
                  }

                  // xy cross
                  if (x != 1 && y != 1)
                  {
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 2][y + 2][z + 1], stiffness, how);
                     body.createLocalSpring(grid[x + 2][y + 1][z + 1], grid[x + 1][y + 2][z + 1], stiffness, how);
                  }

                  // zy cross
                  if (y != 1 && z != 1)
                  {
                     body.createLocalSpring(grid[x + 1][y + 1][z + 1], grid[x + 1][y + 2][z + 2], stiffness, how);
                     body.createLocalSpring(grid[x + 1][y + 1][z + 2], grid[x + 1][y + 2][z + 1], stiffness, how);
                  }
               }
            }
         }
      }
      else
      {
         VerletParticle center = new VerletParticle();
         VerletSphere sphere = new VerletSphere(center, centerRadius);
         center.setPosition(xOff, yOff, zOff);
         body.addSphere(sphere);

         List<VerletSphere> spheres = body.listSpheres();
         for (int i = 0; i < spheres.size(); i++)
            for (int k = i + 1; k < spheres.size(); k++)
               body.createLocalSpring(spheres.get(i), spheres.get(k), stiffness, how);
      }

      return body;
   }
}
